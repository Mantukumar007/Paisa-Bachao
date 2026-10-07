package com.example

import com.example.data.model.Transaction
import com.example.data.model.TransactionEntity
import com.example.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTransactionEntity_offlineCreation() {
        val txn: Transaction = TransactionEntity(
            amount = 250.0,
            type = "EXPENSE",
            category = "Food & Dining",
            merchant = "Local Chai Shop",
            source = "MANUAL_CASH",
            accountOrBank = "Cash in Hand",
            isSyncedToCloud = false
        )

        assertEquals(250.0, txn.amount, 0.01)
        assertEquals("MANUAL_CASH", txn.source)
        assertFalse(txn.isSyncedToCloud)
    }

    @Test
    fun testSmsParser_hdfcUpiDebit() {
        val sms = "Dear Customer, INR 450.00 debited from A/c XX4512 on 06-OCT-26 towards Swiggy UPI. Avl Bal: INR 18,240.00."
        val result = SmsParser.parse(sms, "VK-HDFCBK")

        assertTrue(result.isBankTransaction)
        assertEquals(450.0, result.amount, 0.01)
        assertEquals("EXPENSE", result.type)
        assertEquals("Swiggy", result.merchant)
        assertEquals("Food & Dining", result.category)
        assertTrue(result.accountOrBank.contains("HDFC"))
    }

    @Test
    fun testSmsParser_sbiAtmCashWithdrawal() {
        val sms = "Txn of Rs. 2,500.00 with SBI Debit Card XX3311 done at HDFC ATM CASH WDL on 04-OCT-26. Avl Bal: Rs 12,000."
        val result = SmsParser.parse(sms, "AX-SBIINB")

        assertTrue(result.isBankTransaction)
        assertEquals(2500.0, result.amount, 0.01)
        assertEquals("EXPENSE", result.type)
        assertEquals("Cash & ATM", result.category)
    }

    @Test
    fun testSmsParser_salaryCredit() {
        val sms = "Your A/C 9876 is credited with Rs 45,000.00 on 01-OCT-26 by Salary NEFT-INFY. Avl Bal Rs 52,100."
        val result = SmsParser.parse(sms, "VM-AXISBK")

        assertTrue(result.isBankTransaction)
        assertEquals(45000.0, result.amount, 0.01)
        assertEquals("INCOME", result.type)
        assertEquals("Salary", result.category)
    }

    @Test
    fun testSmsParser_ignoreOtpMessages() {
        val sms = "Your OTP for login to netbanking is 482910. Do not share this OTP with anyone."
        val result = SmsParser.parse(sms, "VM-HDFCBK")

        assertFalse(result.isBankTransaction)
        assertTrue(result.ignoreReason.contains("OTP", ignoreCase = true))
    }

    @Test
    fun testSmsParser_acceptanceTestScenario() {
        val sms = "UTR: ABC123456\nAmount: ₹500\nMerchant: ABC Store"
        val result = SmsParser.parse(sms, "AX-TEST")

        assertTrue(result.isBankTransaction)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals("ABC123456", result.referenceId)
        assertEquals("ABC Store", result.merchant)
        assertEquals("EXPENSE", result.type)
    }

    @Test
    fun testSmsParser_priority1_utrUpi() {
        val sms = "Paid Rs 750 to Zomato via UPI. UTR: HDFC998877. Avl Bal Rs 5000."
        val result = SmsParser.parse(sms, "VK-HDFCBK")

        assertTrue(result.isBankTransaction)
        assertEquals(750.0, result.amount, 0.01)
        assertEquals("HDFC998877", result.referenceId)
    }

    @Test
    fun testSmsParser_priority2_bankTxnId() {
        val sms = "Txn ID: TXN445566 of INR 1200 debited from A/c XX1234 towards Amazon on 06-OCT-26."
        val result = SmsParser.parse(sms, "BZ-ICICIB")

        assertTrue(result.isBankTransaction)
        assertEquals(1200.0, result.amount, 0.01)
        assertEquals("TXN445566", result.referenceId)
    }

    @Test
    fun testSmsParser_priority3_referenceNumber() {
        val sms = "Paid Rs.350 to Uber India using UPI. Ref no 3482910. Avl Bal INR 4,120."
        val result = SmsParser.parse(sms, "BW-PAYTMB")

        assertTrue(result.isBankTransaction)
        assertEquals(350.0, result.amount, 0.01)
        assertEquals("3482910", result.referenceId)
    }

    @Test
    fun testSmsParser_priorityPrecedence_utrWinsOverRefNo() {
        val sms = "Debited INR 500 towards Swiggy. UTR: ABC123456. Ref no: 998877."
        val ref = SmsParser.extractTransactionReference(sms)
        assertEquals("ABC123456", ref)
    }
}
