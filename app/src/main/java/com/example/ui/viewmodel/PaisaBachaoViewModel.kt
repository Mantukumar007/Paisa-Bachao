package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.PaisaBachaoRepository
import com.example.data.repository.TransactionInsertResult
import com.example.security.SecurityManager
import com.example.sms.SmsReaderHelper
import com.example.sync.StorageUsageInfo
import com.example.sync.SyncBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val percentageOfTotal: Float,
    val count: Int
)

data class DaySpend(
    val dayLabel: String,
    val amount: Double
)

data class BudgetAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percent: Int,
    val isExceeded: Boolean
)

data class DashboardSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val todayExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRatePercent: Int = 0,
    val categorySpends: List<CategorySpend> = emptyList(),
    val weeklySpends: List<DaySpend> = emptyList(),
    val budgetAlerts: List<BudgetAlert> = emptyList(),
    val overallBudget: Double = 0.0,
    val overallSpent: Double = 0.0
)

class PaisaBachaoViewModel(
    private val repository: PaisaBachaoRepository,
    val securityManager: SecurityManager,
    val syncBackupManager: SyncBackupManager,
    val smsReaderHelper: SmsReaderHelper,
    val biometricAuthManager: com.example.security.BiometricAuthManager
) : ViewModel() {

    // First-run user consent state
    private val _hasUserConsented = MutableStateFlow(securityManager.hasUserConsented())
    val hasUserConsented: StateFlow<Boolean> = _hasUserConsented.asStateFlow()

    // PIN lock state
    private val _isLocked = MutableStateFlow(securityManager.isAppLockEnabled() && securityManager.hasPinSet())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _isBalanceMasked = MutableStateFlow(securityManager.isBalanceMasked())
    val isBalanceMasked: StateFlow<Boolean> = _isBalanceMasked.asStateFlow()

    // Filters and Search
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME", "CASH", "SMS"
    val selectedCategoryFilter = MutableStateFlow("ALL")

    // Status banner
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isScanningSms = MutableStateFlow(false)
    val isScanningSms: StateFlow<Boolean> = _isScanningSms.asStateFlow()

    // Raw Flows from Room (100% on-device local database)
    val transactions = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentTransactions = repository.recentTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val budgets = repository.allBudgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val goals = repository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val smsLogs = repository.allSmsLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered Transactions
    val filteredTransactions = combine(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter
    ) { txns, query, typeFilter, catFilter ->
        txns.filter { txn ->
            val matchesQuery = query.isBlank() ||
                    txn.merchant.contains(query, ignoreCase = true) ||
                    txn.category.contains(query, ignoreCase = true) ||
                    txn.notes.contains(query, ignoreCase = true) ||
                    txn.accountOrBank.contains(query, ignoreCase = true) ||
                    (txn.referenceId?.contains(query, ignoreCase = true) == true)

            val matchesType = when (typeFilter) {
                "EXPENSE" -> txn.type == "EXPENSE"
                "INCOME" -> txn.type == "INCOME"
                "CASH" -> txn.source == "MANUAL_CASH"
                "SMS" -> txn.source == "SMS_AUTO"
                else -> true
            }

            val matchesCat = catFilter == "ALL" || txn.category.equals(catFilter, ignoreCase = true)

            matchesQuery && matchesType && matchesCat
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dashboard Analytics computation (Local-only on device)
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        transactions,
        budgets
    ) { txns, budgetList ->
        computeDashboardSummary(txns, budgetList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardSummary()
    )

    private fun computeDashboardSummary(
        txns: List<TransactionEntity>,
        budgetList: List<BudgetEntity>
    ): DashboardSummary {
        val now = System.currentTimeMillis()

        // Month Start
        val monthCal = Calendar.getInstance()
        monthCal.timeInMillis = now
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        monthCal.set(Calendar.HOUR_OF_DAY, 0)
        monthCal.set(Calendar.MINUTE, 0)
        monthCal.set(Calendar.SECOND, 0)
        monthCal.set(Calendar.MILLISECOND, 0)
        val monthStart = monthCal.timeInMillis

        // Today Start
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = now
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis

        val currentMonthTxns = txns.filter { it.timestamp >= monthStart }
        val todayTxns = txns.filter { it.timestamp >= todayStart }

        var totalInc = 0.0
        var totalExp = 0.0
        val categoryTotals = mutableMapOf<String, Double>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (t in txns) {
            if (t.type == "INCOME") {
                totalInc += t.amount
            } else {
                totalExp += t.amount
                val cat = t.category.ifBlank { "Other Expenses" }
                categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + t.amount
                categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
            }
        }

        val thisMonthExp = currentMonthTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val todayExp = todayTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val netSavings = totalInc - totalExp
        val savingsRate = if (totalInc > 0) ((netSavings.coerceAtLeast(0.0) / totalInc) * 100).toInt() else 0

        val categorySpends = categoryTotals.map { (cat, amt) ->
            val pct = if (totalExp > 0) (amt / totalExp).toFloat() else 0f
            CategorySpend(cat, amt, pct, categoryCounts[cat] ?: 1)
        }.sortedByDescending { it.totalAmount }

        // Last 7 days spend trend
        val daySpends = mutableListOf<DaySpend>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = now
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dStart = dayCal.timeInMillis
            val dEnd = dStart + (24L * 60 * 60 * 1000)

            val dSpent = txns.filter { it.type == "EXPENSE" && it.timestamp in dStart until dEnd }
                .sumOf { it.amount }
            daySpends.add(DaySpend(dayFormat.format(Date(dStart)), dSpent))
        }

        // Budget Alerts
        val alerts = mutableListOf<BudgetAlert>()
        var overallBudgetLimit = 0.0
        for (b in budgetList) {
            val spentInCat = if (b.category.contains("Overall", ignoreCase = true)) {
                overallBudgetLimit = b.monthlyLimit
                thisMonthExp
            } else {
                currentMonthTxns.filter { it.type == "EXPENSE" && it.category.equals(b.category, ignoreCase = true) }
                    .sumOf { it.amount }
            }
            val pct = if (b.monthlyLimit > 0) ((spentInCat / b.monthlyLimit) * 100).toInt() else 0
            if (pct >= b.alertThresholdPercent) {
                alerts.add(
                    BudgetAlert(
                        category = b.category,
                        spent = spentInCat,
                        limit = b.monthlyLimit,
                        percent = pct,
                        isExceeded = pct >= 100
                    )
                )
            }
        }

        return DashboardSummary(
            totalIncome = totalInc,
            totalExpense = totalExp,
            thisMonthExpense = thisMonthExp,
            todayExpense = todayExp,
            netSavings = netSavings,
            savingsRatePercent = savingsRate,
            categorySpends = categorySpends,
            weeklySpends = daySpends,
            budgetAlerts = alerts,
            overallBudget = overallBudgetLimit,
            overallSpent = thisMonthExp
        )
    }

    // First-run Consent Action
    fun grantUserConsent() {
        securityManager.setUserConsented(true)
        _hasUserConsented.value = true
        showStatus("Welcome to Paisa Bachao!")
    }

    // App Lock Actions
    fun enterPinDigit(digit: String) {
        if (_pinInput.value.length < 4) {
            _pinInput.value += digit
            _pinError.value = null
            if (_pinInput.value.length == 4) {
                verifyPin()
            }
        }
    }

    fun deletePinDigit() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _pinError.value = null
        }
    }

    private fun verifyPin() {
        val pin = _pinInput.value
        if (securityManager.verifyPin(pin)) {
            _isLocked.value = false
            _pinInput.value = ""
            _pinError.value = null
        } else {
            _pinError.value = "Incorrect PIN. Please try again."
            _pinInput.value = ""
        }
    }

    fun unlockViaBiometric() {
        _isLocked.value = false
        _pinInput.value = ""
        _pinError.value = null
    }

    fun setLockEnabled(enabled: Boolean, pin: String = "") {
        if (enabled && pin.isNotBlank()) {
            securityManager.setPin(pin)
            securityManager.setAppLockEnabled(true)
        } else if (!enabled) {
            securityManager.clearPin()
            _isLocked.value = false
        }
    }

    fun lockAppNow() {
        if (securityManager.isAppLockEnabled() && securityManager.hasPinSet()) {
            _isLocked.value = true
            _pinInput.value = ""
        }
    }

    fun toggleBalanceMask() {
        val newVal = !_isBalanceMasked.value
        _isBalanceMasked.value = newVal
        securityManager.setBalanceMasked(newVal)
    }

    // Transaction Management (100% on-device Room with centralized duplicate prevention)
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        merchant: String,
        accountOrBank: String,
        source: String,
        notes: String = "",
        referenceId: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val cleanRef = referenceId?.trim()?.ifBlank { null }
            val txn = TransactionEntity(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                accountOrBank = accountOrBank,
                source = source,
                notes = notes,
                referenceId = cleanRef,
                timestamp = timestamp
            )

            when (val result = repository.insertTransactionSafely(txn)) {
                is TransactionInsertResult.Success -> {
                    showStatus("Saved locally: ₹${"%,.2f".format(amount)}")
                }
                is TransactionInsertResult.Duplicate -> {
                    showStatus(result.reason)
                }
                is TransactionInsertResult.Error -> {
                    showStatus(result.message)
                }
            }
        }
    }

    fun updateTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(txn)
            showStatus("Transaction updated locally")
        }
    }

    fun deleteTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(txn)
            showStatus("Transaction removed from device")
        }
    }

    // Budget Management
    fun addOrUpdateBudget(category: String, limit: Double, alertThreshold: Int = 80) {
        viewModelScope.launch {
            val current = budgets.value.find { it.category.equals(category, ignoreCase = true) }
            if (current != null) {
                repository.updateBudget(current.copy(monthlyLimit = limit, alertThresholdPercent = alertThreshold))
            } else {
                repository.insertBudget(BudgetEntity(category = category, monthlyLimit = limit, alertThresholdPercent = alertThreshold))
            }
            showStatus("Local budget for $category set to ₹${"%,.2f".format(limit)}")
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            showStatus("Budget removed")
        }
    }

    // Savings Goals Management
    fun addGoal(title: String, targetAmount: Double, targetDate: Long, category: String) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDate = targetDate,
                category = category
            )
            repository.insertGoal(goal)
            showStatus("Goal '$title' created locally!")
        }
    }

    fun contributeToGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            showStatus("Added ₹${"%,.2f".format(amount)} to '${goal.title}'! 🎯")
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            showStatus("Goal removed")
        }
    }

    // SMS Scanning (100% on-device parsing)
    fun scanDeviceSmsInbox() {
        viewModelScope.launch {
            _isScanningSms.value = true
            val result = smsReaderHelper.scanDeviceInbox()
            _isScanningSms.value = false
            showStatus(result.message)
        }
    }

    fun simulateBankSms(smsBody: String, sender: String = "VK-HDFCBK") {
        viewModelScope.launch {
            val (_, msg) = smsReaderHelper.processSingleSms(smsBody, sender)
            showStatus(msg)
        }
    }

    // Local Data Management & Explicit User Backups
    suspend fun getStorageUsageInfo(): StorageUsageInfo {
        return syncBackupManager.getStorageUsageInfo()
    }

    suspend fun getBackupJson(): String {
        return syncBackupManager.exportBackupJson()
    }

    fun createExportShareIntent(context: Context) {
        viewModelScope.launch {
            try {
                val intent = syncBackupManager.createExportShareIntent(context)
                context.startActivity(intent)
                showStatus("Local backup exported! Ready to share or save.")
            } catch (e: Exception) {
                showStatus("Export error: ${e.localizedMessage}")
            }
        }
    }

    fun restoreBackup(json: String) {
        viewModelScope.launch {
            val (_, msg) = syncBackupManager.restoreFromJson(json)
            showStatus(msg)
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString.isNullOrBlank()) {
                    showStatus("Selected backup file is empty")
                    return@launch
                }
                val (_, msg) = syncBackupManager.restoreFromJson(jsonString)
                showStatus(msg)
            } catch (e: Exception) {
                showStatus("Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showStatus("All transactions cleared from local storage.")
        }
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }
}

class PaisaBachaoViewModelFactory(
    private val repository: PaisaBachaoRepository,
    private val securityManager: SecurityManager,
    private val syncBackupManager: SyncBackupManager,
    private val smsReaderHelper: SmsReaderHelper,
    private val biometricAuthManager: com.example.security.BiometricAuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaisaBachaoViewModel(
            repository,
            securityManager,
            syncBackupManager,
            smsReaderHelper,
            biometricAuthManager
        ) as T
    }
}
