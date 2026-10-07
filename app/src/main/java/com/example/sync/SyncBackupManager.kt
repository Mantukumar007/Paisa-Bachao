package com.example.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.PaisaBachaoRepository
import com.example.data.repository.TransactionInsertResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StorageUsageInfo(
    val transactionCount: Int,
    val budgetCount: Int,
    val goalCount: Int,
    val smsLogCount: Int,
    val databaseSizeBytes: Long,
    val lastBackupFormatted: String
)

/**
 * Manages 100% on-device local data storage, explicit user backups, and exports.
 * Strictly local-only: No transaction or SMS data is ever transmitted to a server.
 */
class SyncBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: PaisaBachaoRepository = PaisaBachaoRepository(database)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("paisa_bachao_local_data_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_BACKUP_TIME = "pref_last_backup_time"
        private const val PREF_DEVICE_ID = "pref_device_id"
    }

    init {
        if (!prefs.contains(PREF_DEVICE_ID)) {
            prefs.edit().putString(PREF_DEVICE_ID, "PB-" + UUID.randomUUID().toString().take(8).uppercase()).apply()
        }
    }

    fun getDeviceId(): String = prefs.getString(PREF_DEVICE_ID, "PB-DEVICE") ?: "PB-DEVICE"

    fun getLastBackupTime(): Long = prefs.getLong(PREF_LAST_BACKUP_TIME, 0L)

    fun getFormattedLastBackup(): String {
        val last = getLastBackupTime()
        if (last == 0L) return "Never exported"
        val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return "Last export: " + df.format(Date(last))
    }

    fun getFormattedLastSync(): String = getFormattedLastBackup()

    suspend fun getStorageUsageInfo(): StorageUsageInfo = withContext(Dispatchers.IO) {
        val txns = database.dao().getAllTransactions().first()
        val budgets = database.dao().getAllBudgets().first()
        val goals = database.dao().getAllGoals().first()
        val smsLogs = database.dao().getAllSmsLogs().first()

        // Local SQLite file size
        val dbFile = context.getDatabasePath("paisa_bachao_database")
        val size = if (dbFile.exists()) dbFile.length() else 0L

        StorageUsageInfo(
            transactionCount = txns.size,
            budgetCount = budgets.size,
            goalCount = goals.size,
            smsLogCount = smsLogs.size,
            databaseSizeBytes = size,
            lastBackupFormatted = getFormattedLastBackup()
        )
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        database.dao().deleteAllTransactions()
    }

    // Export entire local database to formatted JSON string
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Paisa Bachao")
        root.put("storageMode", "100% Local On-Device")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("deviceId", getDeviceId())

        // Transactions
        val txns = database.dao().getAllTransactions().first()
        val txnsArray = JSONArray()
        for (t in txns) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("amount", t.amount)
            obj.put("type", t.type)
            obj.put("category", t.category)
            obj.put("source", t.source)
            obj.put("merchant", t.merchant)
            obj.put("accountOrBank", t.accountOrBank)
            obj.put("timestamp", t.timestamp)
            obj.put("notes", t.notes)
            obj.put("referenceId", t.referenceId ?: "")
            txnsArray.put(obj)
        }
        root.put("transactions", txnsArray)

        // Budgets
        val budgets = database.dao().getAllBudgets().first()
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject()
            obj.put("category", b.category)
            obj.put("monthlyLimit", b.monthlyLimit)
            obj.put("monthYear", b.monthYear)
            obj.put("alertThresholdPercent", b.alertThresholdPercent)
            budgetsArray.put(obj)
        }
        root.put("budgets", budgetsArray)

        // Goals
        val goals = database.dao().getAllGoals().first()
        val goalsArray = JSONArray()
        for (g in goals) {
            val obj = JSONObject()
            obj.put("title", g.title)
            obj.put("targetAmount", g.targetAmount)
            obj.put("currentAmount", g.currentAmount)
            obj.put("targetDate", g.targetDate)
            obj.put("category", g.category)
            obj.put("isAchieved", g.isAchieved)
            goalsArray.put(obj)
        }
        root.put("goals", goalsArray)

        prefs.edit().putLong(PREF_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()
        root.toString(2)
    }

    // Write backup JSON to local storage cache file and launch Share intent
    suspend fun createExportShareIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        val jsonString = exportBackupJson()
        val file = File(context.cacheDir, "paisa_bachao_backup.json")
        file.writeText(jsonString)

        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Paisa Bachao Financial Data Backup (Local)")
            putExtra(Intent.EXTRA_TEXT, jsonString)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // Restore database from user-provided JSON string with centralized duplicate protection
    suspend fun restoreFromJson(jsonStr: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val txnsArray = root.optJSONArray("transactions")
            var importedTxns = 0
            var duplicatesSkipped = 0

            if (txnsArray != null) {
                for (i in 0 until txnsArray.length()) {
                    val obj = txnsArray.getJSONObject(i)
                    val refId = obj.optString("referenceId", "").trim().ifBlank { null }
                    val txn = TransactionEntity(
                        amount = obj.optDouble("amount", 0.0),
                        type = obj.optString("type", "EXPENSE"),
                        category = obj.optString("category", "Other Expenses"),
                        source = obj.optString("source", "RESTORE"),
                        merchant = obj.optString("merchant", "Merchant"),
                        accountOrBank = obj.optString("accountOrBank", "Cash in Hand"),
                        referenceId = refId,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                    // Centralized duplicate check & insertion
                    val insertResult = repository.insertTransactionSafely(txn)
                    if (insertResult is TransactionInsertResult.Success) {
                        importedTxns++
                    } else if (insertResult is TransactionInsertResult.Duplicate) {
                        duplicatesSkipped++
                    }
                }
            }

            val goalsArray = root.optJSONArray("goals")
            var importedGoals = 0
            if (goalsArray != null) {
                for (i in 0 until goalsArray.length()) {
                    val obj = goalsArray.getJSONObject(i)
                    val goal = SavingsGoalEntity(
                        title = obj.optString("title", "Goal"),
                        targetAmount = obj.optDouble("targetAmount", 1000.0),
                        currentAmount = obj.optDouble("currentAmount", 0.0),
                        targetDate = obj.optLong("targetDate", System.currentTimeMillis()),
                        category = obj.optString("category", "Savings"),
                        isAchieved = obj.optBoolean("isAchieved", false)
                    )
                    database.dao().insertGoal(goal)
                    importedGoals++
                }
            }

            val budgetsArray = root.optJSONArray("budgets")
            var importedBudgets = 0
            if (budgetsArray != null) {
                for (i in 0 until budgetsArray.length()) {
                    val obj = budgetsArray.getJSONObject(i)
                    val b = BudgetEntity(
                        category = obj.optString("category", "Overall"),
                        monthlyLimit = obj.optDouble("monthlyLimit", 25000.0),
                        alertThresholdPercent = obj.optInt("alertThresholdPercent", 80)
                    )
                    database.dao().insertBudget(b)
                    importedBudgets++
                }
            }

            Pair(
                true,
                "Restored $importedTxns transactions" +
                        (if (duplicatesSkipped > 0) " ($duplicatesSkipped duplicates skipped)" else "") +
                        ", $importedBudgets budgets & $importedGoals goals."
            )
        } catch (e: Exception) {
            Pair(false, "Failed to restore backup: ${e.localizedMessage}")
        }
    }

    // Seed initial Indian banking starter data if empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = database.dao().getAllTransactions().first()
        if (existing.isNotEmpty()) return@withContext

        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // Starter transactions reflecting realistic Indian transactions
        val sampleTxns = listOf(
            TransactionEntity(
                amount = 65000.0,
                type = "INCOME",
                category = "Salary",
                source = "SMS_AUTO",
                merchant = "TechCorp Infotech",
                accountOrBank = "HDFC Bank XX4512",
                referenceId = "HDFC998811",
                timestamp = now - (6 * oneDay),
                notes = "Monthly Salary Credited"
            ),
            TransactionEntity(
                amount = 450.0,
                type = "EXPENSE",
                category = "Food & Dining",
                source = "SMS_AUTO",
                merchant = "Swiggy UPI",
                accountOrBank = "HDFC Bank XX4512",
                referenceId = "UPI884422",
                timestamp = now - (4 * oneDay),
                notes = "Dinner order via Swiggy"
            ),
            TransactionEntity(
                amount = 1420.0,
                type = "EXPENSE",
                category = "Groceries",
                source = "SMS_AUTO",
                merchant = "Blinkit Commerce",
                accountOrBank = "SBI XX9012",
                referenceId = "SBI773311",
                timestamp = now - (3 * oneDay),
                notes = "Weekly grocery & milk delivery"
            ),
            TransactionEntity(
                amount = 320.0,
                type = "EXPENSE",
                category = "Transport & Fuel",
                source = "SMS_AUTO",
                merchant = "Uber India",
                accountOrBank = "Paytm Bank UPI",
                referenceId = "PAYTM662244",
                timestamp = now - (2 * oneDay),
                notes = "Cab ride to Tech Park"
            ),
            TransactionEntity(
                amount = 1999.0,
                type = "EXPENSE",
                category = "Bills & Utilities",
                source = "SMS_AUTO",
                merchant = "Airtel Broadband",
                accountOrBank = "ICICI Bank XX8012",
                referenceId = "ICICI551133",
                timestamp = now - (1 * oneDay),
                notes = "Monthly Fiber WiFi recharge"
            ),
            TransactionEntity(
                amount = 500.0,
                type = "EXPENSE",
                category = "Food & Dining",
                source = "MANUAL_CASH",
                merchant = "Local Street Food & Chai",
                accountOrBank = "Cash in Hand",
                referenceId = null,
                timestamp = now - (12 * 60 * 60 * 1000),
                notes = "Evening snacks with friends"
            )
        )
        database.dao().insertTransactions(sampleTxns)

        // Starter Budgets
        val sampleBudgets = listOf(
            BudgetEntity(category = "Overall Monthly", monthlyLimit = 35000.0, alertThresholdPercent = 80),
            BudgetEntity(category = "Food & Dining", monthlyLimit = 6000.0, alertThresholdPercent = 80),
            BudgetEntity(category = "Groceries", monthlyLimit = 8000.0, alertThresholdPercent = 80),
            BudgetEntity(category = "Bills & Utilities", monthlyLimit = 5000.0, alertThresholdPercent = 85),
            BudgetEntity(category = "Shopping", monthlyLimit = 7000.0, alertThresholdPercent = 75),
            BudgetEntity(category = "Transport & Fuel", monthlyLimit = 4000.0, alertThresholdPercent = 80)
        )
        for (b in sampleBudgets) {
            database.dao().insertBudget(b)
        }

        // Starter Savings Goals
        val sampleGoals = listOf(
            SavingsGoalEntity(
                title = "Emergency Fund (6 Months)",
                targetAmount = 150000.0,
                currentAmount = 65000.0,
                targetDate = now + (180L * oneDay),
                category = "Security",
                iconName = "shield"
            ),
            SavingsGoalEntity(
                title = "New Electric Scooter",
                targetAmount = 85000.0,
                currentAmount = 32000.0,
                targetDate = now + (90L * oneDay),
                category = "Vehicle",
                iconName = "motorcycle"
            ),
            SavingsGoalEntity(
                title = "Goa Vacation Trip",
                targetAmount = 25000.0,
                currentAmount = 14500.0,
                targetDate = now + (45L * oneDay),
                category = "Travel",
                iconName = "flight"
            )
        )
        for (g in sampleGoals) {
            database.dao().insertGoal(g)
        }
    }
}
