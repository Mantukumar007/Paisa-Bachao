# Paisa Bachao (पैसा बचाओ) 💰

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Storage-Room%20(SQLite)-009688.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Biometric Auth](https://img.shields.io/badge/Security-BiometricPrompt%20%2B%20AES-F59E0B.svg?style=flat)](https://developer.android.com/training/sign-in/biometric-auth)
[![100% Local](https://img.shields.io/badge/Privacy-100%25%20On--Device%20(No%20Cloud)-10B981.svg?style=flat)](https://github.com)

**Paisa Bachao** is a modern, privacy-focused, 100% on-device personal expense manager and automated bank SMS transaction tracker for Android. Built with Jetpack Compose and Material Design 3, it allows users to track expenses, manage budgets, track savings goals, and automatically categorize bank transactions without ever sending financial data to the cloud.

---

## 🌟 Key Highlights

- **🔒 100% On-Device & Local-First**: No external servers, no cloud databases (Firebase/Supabase), and no remote analytics collecting your finances. Everything runs locally on SQLite Room.
- **✈️ Works Completely Offline**: Zero internet dependency. Fully functional in Airplane Mode or without network access.
- **📩 Smart Bank SMS Transaction Detection**: Parses debit and credit SMS notifications from Indian banks (SBI, HDFC, ICICI, Axis, PNB, Kotak, etc.) and UPI applications (PhonePe, Google Pay, Paytm, CRED).
- **🛡️ Duplicate Transaction Prevention**: Centralized multi-layer detection prioritizing UTR / UPI Transaction IDs, Bank Transaction IDs, and reference numbers with database-level `UNIQUE` constraints.
- **👆 Biometric App Lock & Privacy**: Protected with Android's `BiometricPrompt` API (Fingerprint / Face Unlock), 4-digit PIN fallback, and balance masking (`₹••••`) to prevent shoulder surfing.
- **📊 Real-Time Analytics & Budgets**: Category spend breakdowns, weekly spending trends, custom monthly budget limits with warning alerts, and savings goal trackers.
- **💾 User-Controlled Backup & Export**: Export encrypted JSON backups to local storage or share via system share sheet on demand.

---

## 📱 Application Flow & Screenshots

```text
               ┌────────────────────────┐
               │  Animated Splash Screen │ (Spring bounce emblem & ambient glow)
               └───────────┬────────────┘
                           │
                           ▼
               ┌────────────────────────┐
               │ First-Run Consent Screen│ (Privacy Policy & Terms checkboxes)
               └───────────┬────────────┘
                           │
                           ▼
               ┌────────────────────────┐
               │ Biometric / PIN Lock   │ (Optional security overlay)
               └───────────┬────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Main App Navigation                         │
├─────────────┬──────────────┬───────────┬───────────┬─────────────┤
│  Dashboard  │ Transactions │  Budgets  │   Goals   │ SMS Insights│
└─────────────┴──────────────┴───────────┴───────────┴─────────────┘
                                                      │
                                                      ▼
                                         ┌─────────────────────────┐
                                         │ Settings & Privacy      │
                                         ├─────────────────────────┤
                                         │ • Biometric Toggle      │
                                         │ • Balance Masking       │
                                         │ • Local Data Export     │
                                         │ • Privacy Policy Page   │
                                         │ • Terms & Conditions    │
                                         └─────────────────────────┘
```

---

## ✨ Features

### 1. Automated & Manual Expense Tracking
- Track cash expenses, card swipes, ATM withdrawals, and UPI transactions.
- Filter by type (`ALL`, `EXPENSE`, `INCOME`, `CASH`, `SMS`) or categories (*Food & Dining, Groceries, Shopping, Bills & Utilities, Transport, Cash & ATM, Healthcare, Salary*).
- Search instantly across merchant names, notes, and transaction references.

### 2. Bank SMS Intelligence & Simulator
- Automatically reads and parses bank SMS notifications locally when SMS permission is granted.
- **Built-in SMS Simulator**: Test SMS extraction with pre-populated templates (HDFC UPI, SBI ATM withdrawal, ICICI debit, Axis salary credit) without needing live SMS messages.
- Encrypts sensitive raw SMS bodies at rest using device-derived AES encryption.

### 3. Duplicate Prevention Engine
Transactions pass through a strict duplicate check before database insertion:
1. **Priority 1**: UTR / UPI Transaction ID (`UTR: ABC123456`, `UPI Ref: 412345678901`)
2. **Priority 2**: Bank Transaction ID (`Txn ID: TXN445566`)
3. **Priority 3**: Reference Number (`Ref: REF998877`)
4. **Fallback**: Exact match on Amount + Merchant + Transaction Type + Timestamp (within 60-second window).
- Room database enforces a `UNIQUE` index on `referenceId`. Re-scanned SMS messages return `DUPLICATE` without crashing or creating duplicate records.

### 4. Biometric Security & App Lock
- Integrated with AndroidX `BiometricPrompt` API supporting strong biometric credentials.
- 4-digit PIN lock fallback with brute-force delay protection.
- Quick "Lock App Now" action in Settings or Dashboard header.
- Privacy mode toggle to mask sensitive account balances on the dashboard.

### 5. Clear Legal & Privacy Disclosures
- **First-Run Consent Screen**: Clean onboarding displaying clear summaries of data practices, direct links to review the Privacy Policy and Terms & Conditions, and non-pre-checked agreement checkboxes before granting access.
- Dedicated **Privacy Policy** and **Terms & Conditions** screens accessible at any time from **Settings**.

---

## 🏗️ Architecture & Tech Stack

The app follows modern Android Architecture Components and clean separation of concerns:

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Programming Language**: [Kotlin](https://kotlinlang.org/) (Coroutines + StateFlow)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP compiler
- **Security & Cryptography**: Android KeyStore, `BiometricPrompt` (`androidx.biometric`), AES-GCM encryption
- **Asynchronous Flow**: Kotlin Coroutines (`viewModelScope`) + `StateFlow` / `collectAsStateWithLifecycle`
- **Unit & Robolectric Testing**: JUnit 4, Kotlin Coroutines Test, Robolectric 4.16

---

## 📁 Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                     # Application entry point, navigation & dialogs
├── PaisaBachaoApplication.kt          # Application class with lazy singletons
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt             # Room Database configuration
│   │   └── PaisaBachaoDao.kt          # Centralized Data Access Object & queries
│   ├── model/
│   │   ├── BudgetEntity.kt            # Budget model
│   │   ├── SavingsGoalEntity.kt       # Savings goal model
│   │   ├── SmsLogEntity.kt            # Audit log model for parsed SMS
│   │   └── TransactionEntity.kt       # Transaction model with UNIQUE reference index
│   └── repository/
│       └── PaisaBachaoRepository.kt   # Repository & centralized duplicate prevention
├── receiver/
│   └── SmsReceiver.kt                 # BroadcastReceiver for real-time incoming SMS
├── security/
│   ├── BiometricAuthManager.kt        # AndroidX BiometricPrompt helper
│   └── SecurityManager.kt             # PIN, encrypted preferences & AES cipher
├── sms/
│   ├── SmsParser.kt                   # Regex-based local bank SMS parser
│   └── SmsReaderHelper.kt             # Device SMS inbox reader & reconciliation
├── sync/
│   └── SyncBackupManager.kt           # On-device JSON export, import & starter data
└── ui/
    ├── components/
    │   ├── CommonDialogs.kt           # Add Transaction, Budget, Goals, Backup dialogs
    │   └── SecurityComponents.kt      # PIN Pad & App Lock screen
    ├── screens/
    │   ├── AnimatedSplashScreen.kt    # Animated splash screen with spring physics
    │   ├── FirstRunConsentScreen.kt   # First-run onboarding consent screen
    │   ├── DashboardScreen.kt         # Financial overview & quick actions
    │   ├── TransactionsScreen.kt      # Transaction list, search & filters
    │   ├── BudgetsScreen.kt           # Budget management & threshold alerts
    │   ├── GoalsScreen.kt             # Savings goals tracker
    │   ├── SmsInsightsScreen.kt       # SMS reader, logs & bank simulator
    │   ├── SettingsScreen.kt          # Security, backup & legal navigation
    │   ├── PrivacyPolicyScreen.kt     # Dedicated Privacy Policy page
    │   └── TermsAndConditionsScreen.kt# Dedicated Terms & Conditions page
    ├── theme/
    │   ├── Color.kt                   # M3 financial color schemes
    │   ├── Theme.kt                   # Dynamic & static theme definitions
    │   └── Type.kt                    # Typography definitions
    └── viewmodel/
        └── PaisaBachaoViewModel.kt    # Unified ViewModel for UI state management
```

---

## 🛠️ Build & Installation

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Version 17 or 21
- **Android SDK**: Compile SDK 36 (Android 15 / 16 preview compatible), Min SDK 24 (Android 7.0+)

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/paisa-bachao.git
   cd paisa-bachao
   ```
2. Build the debug APK using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests and Robolectric tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Install on a connected Android device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔒 Permissions Policy

| Permission | Type | Usage |
|---|---|---|
| `android.permission.RECEIVE_SMS` | Runtime | Listens for incoming bank SMS alerts in real-time. Processed 100% on-device. |
| `android.permission.READ_SMS` | Runtime | Scans existing bank SMS notifications when the user triggers an inbox sync. |
| `android.permission.USE_BIOMETRIC` | Normal | Allows biometric authentication (Fingerprint / Face) to unlock the app. |
| `android.permission.VIBRATE` | Normal | Provides haptic feedback for PIN pad and biometric events. |
| `android.permission.POST_NOTIFICATIONS`| Runtime | Displays local notifications for budget alerts and newly categorized expenses. |

> **Privacy Guarantee**: This app **never** transmits SMS text, account balances, or financial details to any third-party server, analytics platform, or advertising service.

---

## 👨‍💻 Author

Created with ❤️ by **Mantu Kumar**
- Portfolio: [mantukumar-portfolio.vercel.app](https://mantukumar-portfolio.vercel.app/?utm_source=chatgpt.com)
- Email: cms.mantukumar@gmail.com

---

## 📄 License

This project is open-source software licensed under the [MIT License](LICENSE).
