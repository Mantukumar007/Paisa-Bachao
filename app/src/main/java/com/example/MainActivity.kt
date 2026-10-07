package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AppLockScreen
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.ContributeGoalDialog
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.SetBudgetDialog
import com.example.ui.components.SmsSimulatorDialog
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirstRunConsentScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsInsightsScreen
import com.example.ui.screens.TermsAndConditionsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PaisaBachaoTheme
import com.example.ui.viewmodel.PaisaBachaoViewModel
import com.example.ui.viewmodel.PaisaBachaoViewModelFactory
import kotlinx.coroutines.launch

enum class Screen(val title: String) {
    DASHBOARD("Dashboard"),
    TRANSACTIONS("Transactions"),
    BUDGETS("Budgets"),
    GOALS("Savings Goals"),
    SMS_INSIGHTS("SMS Insights"),
    SETTINGS("Settings"),
    PRIVACY_POLICY("Privacy Policy"),
    TERMS_AND_CONDITIONS("Terms & Conditions")
}

class MainActivity : ComponentActivity() {

    private val viewModel: PaisaBachaoViewModel by viewModels {
        val app = application as PaisaBachaoApplication
        PaisaBachaoViewModelFactory(
            repository = app.repository,
            securityManager = app.securityManager,
            syncBackupManager = app.syncBackupManager,
            smsReaderHelper = app.smsReaderHelper,
            biometricAuthManager = app.biometricAuthManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            PaisaBachaoTheme(darkTheme = isDarkMode) {
                MainAppContent(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = it },
                    onTriggerBiometricAuth = { authenticateBiometrically() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val app = application as PaisaBachaoApplication
        app.biometricAuthManager.cancelAuthentication()
        // Auto-lock on app pause/background
        viewModel.lockAppNow()
    }

    private fun authenticateBiometrically() {
        val app = application as PaisaBachaoApplication
        app.biometricAuthManager.promptBiometric(
            activity = this,
            executor = mainExecutor,
            onSuccess = {
                viewModel.unlockViaBiometric()
                viewModel.showStatus("Biometric authentication verified ✓")
            },
            onError = { errMsg ->
                viewModel.showStatus(errMsg)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: PaisaBachaoViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onTriggerBiometricAuth: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Splash and Consent state
    val hasUserConsented by viewModel.hasUserConsented.collectAsStateWithLifecycle()
    var isSplashVisible by remember { mutableStateOf(true) }
    var consentViewingDocument by remember { mutableStateOf<Screen?>(null) }

    // State collections
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val pinInput by viewModel.pinInput.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()

    // Automatically prompt Biometric on application entry if locked and biometric enabled
    LaunchedEffect(isLocked) {
        if (isLocked && viewModel.securityManager.isBiometricEnabled()) {
            onTriggerBiometricAuth()
        }
    }

    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val rawTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val isBalanceMasked by viewModel.isBalanceMasked.collectAsStateWithLifecycle()
    val isScanningSms by viewModel.isScanningSms.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    // Storage file import launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri)
        }
    }

    // Navigation state
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    // Dialog states
    var showAddTxnDialog by remember { mutableStateOf(false) }
    var addTxnIsCash by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var selectedTxnDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSmsSimulatorDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    // SMS Permission Check
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        viewModel.securityManager.setSmsPermissionGranted(granted)
        if (granted) {
            viewModel.showStatus("SMS permission granted! Scanning inbox for bank messages...")
            viewModel.scanDeviceSmsInbox()
        } else {
            viewModel.showStatus("SMS permission denied. You can still test with the Bank SMS Simulator!")
        }
    }

    // Handle status message snackbars
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatus()
        }
    }

    // 1. Animated Splash Screen
    if (isSplashVisible) {
        AnimatedSplashScreen(
            onSplashFinished = {
                isSplashVisible = false
            }
        )
        return
    }

    // 2. First-Run Consent Screen
    if (!hasUserConsented) {
        when (consentViewingDocument) {
            Screen.PRIVACY_POLICY -> {
                PrivacyPolicyScreen(
                    onNavigateBack = { consentViewingDocument = null }
                )
            }
            Screen.TERMS_AND_CONDITIONS -> {
                TermsAndConditionsScreen(
                    onNavigateBack = { consentViewingDocument = null }
                )
            }
            else -> {
                FirstRunConsentScreen(
                    onViewPrivacyPolicy = { consentViewingDocument = Screen.PRIVACY_POLICY },
                    onViewTermsAndConditions = { consentViewingDocument = Screen.TERMS_AND_CONDITIONS },
                    onConsentGiven = {
                        viewModel.grantUserConsent()
                        if (!hasSmsPermission) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }
                )
            }
        }
        return
    }

    // 3. App Lock Overlay
    if (isLocked) {
        AppLockScreen(
            pinLength = pinInput.length,
            errorMessage = pinError,
            onDigitClick = { viewModel.enterPinDigit(it) },
            onDeleteClick = { viewModel.deletePinDigit() },
            onBiometricClick = onTriggerBiometricAuth
        )
        return
    }

    // Main Scaffold UI
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen != Screen.DASHBOARD && currentScreen != Screen.PRIVACY_POLICY && currentScreen != Screen.TERMS_AND_CONDITIONS) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { currentScreen = Screen.SETTINGS },
                            modifier = Modifier.testTag("top_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors()
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.PRIVACY_POLICY && currentScreen != Screen.TERMS_AND_CONDITIONS) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.TRANSACTIONS,
                    onClick = { currentScreen = Screen.TRANSACTIONS },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("History", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_transactions")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.BUDGETS,
                    onClick = { currentScreen = Screen.BUDGETS },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                    label = { Text("Budgets", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_budgets")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GOALS,
                    onClick = { currentScreen = Screen.GOALS },
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Goals") },
                    label = { Text("Goals", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_goals")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SMS_INSIGHTS,
                    onClick = { currentScreen = Screen.SMS_INSIGHTS },
                    icon = { Icon(Icons.Default.Sms, contentDescription = "SMS Insights") },
                    label = { Text("SMS", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_sms")
                )
            }
        }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        summary = dashboardSummary,
                        recentTransactions = rawTransactions,
                        isBalanceMasked = isBalanceMasked,
                        onToggleBalanceMask = { viewModel.toggleBalanceMask() },
                        onLockApp = { viewModel.lockAppNow() },
                        onOpenCloudSync = { showCloudSyncDialog = true },
                        onOpenAddTransaction = { isCash ->
                            addTxnIsCash = isCash
                            showAddTxnDialog = true
                        },
                        onOpenSmsScan = { currentScreen = Screen.SMS_INSIGHTS },
                        onTransactionClick = { txn -> selectedTxnDetail = txn },
                        onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                        onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                        onNavigateToGoals = { currentScreen = Screen.GOALS }
                    )
                }

                Screen.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = transactions,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        selectedTypeFilter = selectedTypeFilter,
                        onSelectTypeFilter = { viewModel.selectedTypeFilter.value = it },
                        selectedCategoryFilter = selectedCategoryFilter,
                        onSelectCategoryFilter = { viewModel.selectedCategoryFilter.value = it },
                        onTransactionClick = { txn -> selectedTxnDetail = txn },
                        onAddTransactionClick = {
                            addTxnIsCash = false
                            showAddTxnDialog = true
                        }
                    )
                }

                Screen.BUDGETS -> {
                    BudgetsScreen(
                        budgets = budgets,
                        categorySpends = dashboardSummary.categorySpends,
                        totalSpent = dashboardSummary.totalExpense,
                        onAddOrEditBudgetClick = { b ->
                            editingBudget = b
                            showSetBudgetDialog = true
                        },
                        onDeleteBudgetClick = { b -> viewModel.deleteBudget(b) }
                    )
                }

                Screen.GOALS -> {
                    GoalsScreen(
                        goals = goals,
                        onAddGoalClick = { showAddGoalDialog = true },
                        onContributeClick = { goal -> contributingGoal = goal },
                        onDeleteGoalClick = { goal -> viewModel.deleteGoal(goal) }
                    )
                }

                Screen.SMS_INSIGHTS -> {
                    SmsInsightsScreen(
                        hasSmsPermission = hasSmsPermission,
                        isScanning = isScanningSms,
                        isAutoScanEnabled = viewModel.securityManager.isSmsAutoScanEnabled(),
                        smsLogs = smsLogs,
                        onRequestPermission = {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        },
                        onScanInboxNow = {
                            if (hasSmsPermission) {
                                viewModel.scanDeviceSmsInbox()
                            } else {
                                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                            }
                        },
                        onToggleAutoScan = { enabled ->
                            viewModel.securityManager.setSmsAutoScanEnabled(enabled)
                            viewModel.showStatus(if (enabled) "Real-time SMS listener enabled" else "SMS listener paused")
                        },
                        onOpenSimulator = { showSmsSimulatorDialog = true }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        isAppLockEnabled = viewModel.securityManager.isAppLockEnabled(),
                        hasPinSet = viewModel.securityManager.hasPinSet(),
                        isBiometricEnabled = viewModel.securityManager.isBiometricEnabled(),
                        isBalanceMasked = isBalanceMasked,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onOpenPinSetup = { showPinSetupDialog = true },
                        onToggleBiometric = { viewModel.securityManager.setBiometricEnabled(it) },
                        onToggleBalanceMask = { viewModel.toggleBalanceMask() },
                        onLockAppNow = { viewModel.lockAppNow() },
                        onOpenCloudSync = { showCloudSyncDialog = true },
                        onClearAllData = { viewModel.clearAllData() },
                        onTestBiometric = onTriggerBiometricAuth,
                        onOpenPrivacyPolicy = { currentScreen = Screen.PRIVACY_POLICY },
                        onOpenTerms = { currentScreen = Screen.TERMS_AND_CONDITIONS }
                    )
                }

                Screen.PRIVACY_POLICY -> {
                    PrivacyPolicyScreen(
                        onNavigateBack = { currentScreen = Screen.SETTINGS }
                    )
                }

                Screen.TERMS_AND_CONDITIONS -> {
                    TermsAndConditionsScreen(
                        onNavigateBack = { currentScreen = Screen.SETTINGS }
                    )
                }
            }
        }
    }

    // Active Dialogs
    if (showAddTxnDialog) {
        AddTransactionDialog(
            initialIsCash = addTxnIsCash,
            onDismiss = { showAddTxnDialog = false },
            onConfirm = { amount, type, category, merchant, accountOrBank, source, notes, referenceId ->
                viewModel.addTransaction(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    accountOrBank = accountOrBank,
                    source = source,
                    notes = notes,
                    referenceId = referenceId
                )
                showAddTxnDialog = false
            }
        )
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            initialBudget = editingBudget,
            onDismiss = {
                showSetBudgetDialog = false
                editingBudget = null
            },
            onConfirm = { category, limit, alertThreshold ->
                viewModel.addOrUpdateBudget(category, limit, alertThreshold)
                showSetBudgetDialog = false
                editingBudget = null
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, targetAmount, targetDays, category ->
                val targetDate = System.currentTimeMillis() + (targetDays.toLong() * 24 * 60 * 60 * 1000)
                viewModel.addGoal(title, targetAmount, targetDate, category)
                showAddGoalDialog = false
            }
        )
    }

    contributingGoal?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount ->
                viewModel.contributeToGoal(goal, amount)
                contributingGoal = null
            }
        )
    }

    selectedTxnDetail?.let { txn ->
        val decryptedBody = if (txn.rawSmsBodyEncrypted.isNotBlank()) {
            viewModel.securityManager.decrypt(txn.rawSmsBodyEncrypted)
        } else ""

        TransactionDetailDialog(
            transaction = txn,
            decryptedSmsBody = decryptedBody,
            onDismiss = { selectedTxnDetail = null },
            onDelete = {
                viewModel.deleteTransaction(it)
                selectedTxnDetail = null
            }
        )
    }

    if (showSmsSimulatorDialog) {
        SmsSimulatorDialog(
            onDismiss = { showSmsSimulatorDialog = false },
            onTestSms = { body, sender ->
                viewModel.simulateBankSms(body, sender)
                showSmsSimulatorDialog = false
            }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            deviceId = viewModel.syncBackupManager.getDeviceId(),
            lastSyncFormatted = viewModel.syncBackupManager.getFormattedLastBackup(),
            isOnline = false,
            unsyncedCount = 0,
            onDismiss = { showCloudSyncDialog = false },
            onTriggerSync = {
                viewModel.createExportShareIntent(context)
            },
            onExportJson = {
                coroutineScope.launch {
                    val json = viewModel.getBackupJson()
                    clipboardManager.setText(AnnotatedString(json))
                    viewModel.showStatus("Encrypted backup copied to clipboard!")
                }
            },
            onExportFile = {
                viewModel.createExportShareIntent(context)
            },
            onPickImportFile = {
                importFileLauncher.launch("application/json")
            },
            onImportJson = { json ->
                viewModel.restoreBackup(json)
                showCloudSyncDialog = false
            }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            currentHasPin = viewModel.securityManager.hasPinSet(),
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { pin ->
                viewModel.setLockEnabled(true, pin)
                viewModel.showStatus("4-Digit PIN protection activated!")
                showPinSetupDialog = false
            },
            onRemovePin = {
                viewModel.setLockEnabled(false)
                viewModel.showStatus("PIN protection disabled.")
                showPinSetupDialog = false
            }
        )
    }
}
