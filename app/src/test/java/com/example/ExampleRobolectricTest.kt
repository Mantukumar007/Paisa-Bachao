package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.repository.PaisaBachaoRepository
import com.example.data.repository.TransactionInsertResult
import com.example.security.BiometricAuthManager
import com.example.security.BiometricStatus
import com.example.security.SecurityManager
import com.example.sms.SmsReaderHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Paisa Bachao", appName)
    }

    @Test
    fun `security manager encrypt and decrypt`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)
        val sensitiveSms = "INR 1250 debited from A/c XX8921"
        val encrypted = securityManager.encrypt(sensitiveSms)
        val decrypted = securityManager.decrypt(encrypted)

        assertEquals(sensitiveSms, decrypted)
    }

    @Test
    fun `pin lock verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)
        securityManager.setPin("4321")

        assertTrue(securityManager.verifyPin("4321"))
        assertFalse(securityManager.verifyPin("0000"))
    }

    @Test
    fun `biometric manager check status does not throw`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val biometricAuthManager = BiometricAuthManager(context)
        val status = biometricAuthManager.checkBiometricStatus()
        assertNotNull(status)
    }

    private fun createTestDatabase(context: Context): AppDatabase {
        return androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `acceptance test duplicate transaction prevention with UTR`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createTestDatabase(context)
        try {
            val repo = PaisaBachaoRepository(db)

            val txn = TransactionEntity(
                amount = 500.0,
                merchant = "ABC Store",
                category = "Shopping",
                referenceId = "ABC123456",
                type = "EXPENSE"
            )

            // First scan -> INSERT ✅
            val res1 = repo.insertTransactionSafely(txn)
            assertTrue("First scan should be Success", res1 is TransactionInsertResult.Success)

            // Second scan -> DUPLICATE -> SKIP ❌
            val res2 = repo.insertTransactionSafely(txn)
            assertTrue("Second scan should be Duplicate", res2 is TransactionInsertResult.Duplicate)

            // Third scan -> DUPLICATE -> SKIP ❌
            val res3 = repo.insertTransactionSafely(txn)
            assertTrue("Third scan should be Duplicate", res3 is TransactionInsertResult.Duplicate)

            // Final database result: Only 1 transaction record
            val allTxns = db.dao().getAllTransactions().first()
            assertEquals(1, allTxns.size)
            assertEquals("ABC123456", allTxns[0].referenceId)
            assertEquals(500.0, allTxns[0].amount, 0.01)
            assertEquals("ABC Store", allTxns[0].merchant)
        } finally {
            db.close()
        }
    }

    @Test
    fun `different transactions with different UTR are not duplicates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createTestDatabase(context)
        try {
            val repo = PaisaBachaoRepository(db)

            val txn1 = TransactionEntity(
                amount = 500.0,
                merchant = "ABC Store",
                category = "Shopping",
                referenceId = "ABC123456",
                type = "EXPENSE"
            )
            val txn2 = TransactionEntity(
                amount = 500.0,
                merchant = "ABC Store",
                category = "Shopping",
                referenceId = "XYZ987654",
                type = "EXPENSE"
            )

            val res1 = repo.insertTransactionSafely(txn1)
            val res2 = repo.insertTransactionSafely(txn2)
            assertTrue(res1 is TransactionInsertResult.Success)
            assertTrue(res2 is TransactionInsertResult.Success)

            val allTxns = db.dao().getAllTransactions().first()
            assertEquals(2, allTxns.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `fallback duplicate check without reference within tolerance window`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createTestDatabase(context)
        try {
            val repo = PaisaBachaoRepository(db)
            val now = System.currentTimeMillis()

            val cashTxn1 = TransactionEntity(
                amount = 300.0,
                merchant = "Local Chai",
                category = "Food & Dining",
                timestamp = now,
                referenceId = null,
                source = "MANUAL_CASH"
            )
            val cashTxn2 = TransactionEntity(
                amount = 300.0,
                merchant = "Local Chai",
                category = "Food & Dining",
                timestamp = now + 4000L,
                referenceId = null,
                source = "MANUAL_CASH"
            )

            val res1 = repo.insertTransactionSafely(cashTxn1)
            val res2 = repo.insertTransactionSafely(cashTxn2)

            assertTrue(res1 is TransactionInsertResult.Success)
            assertTrue(res2 is TransactionInsertResult.Duplicate)

            val allTxns = db.dao().getAllTransactions().first()
            assertEquals(1, allTxns.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `processSingleSms repeatedly skips duplicates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createTestDatabase(context)
        try {
            val securityManager = SecurityManager(context)
            val repo = PaisaBachaoRepository(db)
            val helper = SmsReaderHelper(context, db, securityManager, repo)

            val sms = "UTR: ABC123456\nAmount: ₹500\nMerchant: ABC Store"

            val (firstSuccess, _) = helper.processSingleSms(sms, "AX-TEST")
            assertTrue(firstSuccess)

            val (secondSuccess, secondMsg) = helper.processSingleSms(sms, "AX-TEST")
            assertFalse(secondSuccess)
            assertTrue(secondMsg.contains("Duplicate", ignoreCase = true))

            val (thirdSuccess, thirdMsg) = helper.processSingleSms(sms, "AX-TEST")
            assertFalse(thirdSuccess)
            assertTrue(thirdMsg.contains("Duplicate", ignoreCase = true))

            val allTxns = db.dao().getAllTransactions().first()
            assertEquals(1, allTxns.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `first run consent state transition and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)
        securityManager.setUserConsented(false)
        assertFalse(securityManager.hasUserConsented())

        securityManager.setUserConsented(true)
        assertTrue(securityManager.hasUserConsented())
    }

    @Test
    fun `app logo resource loads without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_app_logo)
        assertNotNull(drawable)
    }
}
