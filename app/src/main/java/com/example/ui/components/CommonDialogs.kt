package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val TRANSACTION_CATEGORIES = listOf(
    "Food & Dining",
    "Groceries",
    "Shopping",
    "Bills & Utilities",
    "Transport & Fuel",
    "Entertainment",
    "Health & Medical",
    "Investment",
    "Salary",
    "Cash Withdrawal",
    "Other Expenses"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialIsCash: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, category: String, merchant: String, accountOrBank: String, source: String, notes: String, referenceId: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var isCash by remember { mutableStateOf(initialIsCash) }
    var selectedCategory by remember { mutableStateOf(if (type == "EXPENSE") "Food & Dining" else "Salary") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var referenceId by remember { mutableStateOf("") }
    var accountOrBank by remember { mutableStateOf(if (isCash) "Cash in Hand" else "HDFC Bank XX4512") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isCash) "Add Cash Transaction" else "Add Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher: Expense vs Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            type = "EXPENSE"
                            if (selectedCategory == "Salary") selectedCategory = "Food & Dining"
                        },
                        modifier = Modifier.weight(1f).testTag("dialog_type_expense"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "EXPENSE") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense (₹-)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            type = "INCOME"
                            selectedCategory = "Salary"
                        },
                        modifier = Modifier.weight(1f).testTag("dialog_type_income"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "INCOME") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income (₹+)", fontWeight = FontWeight.Bold)
                    }
                }

                // Payment mode: Cash vs Bank/UPI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCash,
                        onClick = {
                            isCash = false
                            if (accountOrBank == "Cash in Hand") accountOrBank = "Bank Account"
                        },
                        label = { Text("Bank / UPI / Card") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("filter_mode_bank")
                    )

                    FilterChip(
                        selected = isCash,
                        onClick = {
                            isCash = true
                            accountOrBank = "Cash in Hand"
                        },
                        label = { Text("Cash") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("filter_mode_cash")
                    )
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Amount (₹)") },
                    leadingIcon = { Text("₹", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                    singleLine = true
                )

                // Merchant / Payee
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(if (type == "EXPENSE") "Paid To / Merchant" else "Received From") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    placeholder = { Text(if (isCash) "e.g. Local Grocery Shop" else "e.g. Swiggy, Amazon") },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_merchant_input"),
                    singleLine = true
                )

                // Account / Wallet label
                OutlinedTextField(
                    value = accountOrBank,
                    onValueChange = { accountOrBank = it },
                    label = { Text("Account / Wallet") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    placeholder = { Text("e.g. SBI XX1234 or Cash in Hand") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category Selection Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (cat in TRANSACTION_CATEGORIES) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Reference / UTR ID for Bank / Online transactions
                if (!isCash) {
                    OutlinedTextField(
                        value = referenceId,
                        onValueChange = { referenceId = it },
                        label = { Text("UTR / Ref ID / Txn ID (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        placeholder = { Text("e.g. ABC123456 or UPI987654") },
                        modifier = Modifier.fillMaxWidth().testTag("transaction_reference_input"),
                        singleLine = true
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    placeholder = { Text("e.g. Dinner with team") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    val finalMerchant = if (merchant.isBlank()) {
                        if (isCash) "Cash Payment" else "Transaction"
                    } else merchant.trim()

                    val source = if (isCash) "MANUAL_CASH" else "MANUAL_ONLINE"
                    val cleanRef = referenceId.trim().ifBlank { null }
                    onConfirm(amt, type, selectedCategory, finalMerchant, accountOrBank.trim(), source, notes.trim(), cleanRef)
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SetBudgetDialog(
    initialBudget: BudgetEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double, alertThreshold: Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialBudget?.category ?: "Overall Monthly") }
    var limitText by remember { mutableStateOf(initialBudget?.monthlyLimit?.toInt()?.toString() ?: "25000") }
    var alertThreshold by remember { mutableFloatStateOf(initialBudget?.alertThresholdPercent?.toFloat() ?: 80f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Overall Monthly") + TRANSACTION_CATEGORIES.filter { it != "Salary" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialBudget != null) "Edit Budget" else "Set Monthly Budget",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                // Category chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (cat in categories.take(6)) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Monthly Spending Limit (₹)") },
                    leadingIcon = { Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )

                Column {
                    Text(
                        text = "Alert Threshold: ${alertThreshold.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Notify when spending crosses this percentage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        valueRange = 50f..100f,
                        steps = 9
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0.0) {
                        errorMessage = "Please enter a valid budget limit"
                        return@Button
                    }
                    onConfirm(selectedCategory, limit, alertThreshold.toInt())
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, targetDays: Int, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("90") } // default 3 months
    var selectedCategory by remember { mutableStateOf("Emergency Fund") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val goalPresets = listOf(
        "Emergency Fund",
        "New Vehicle / Bike",
        "Vacation Trip",
        "Gadget / Laptop",
        "Festival / Wedding",
        "Home Down Payment"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Savings Goal 🎯", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Goa Vacation Fund") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = {
                        targetText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Target Amount (₹)") },
                    leadingIcon = { Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("goal_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetDays,
                    onValueChange = { targetDays = it.filter { char -> char.isDigit() } },
                    label = { Text("Target Duration (Days)") },
                    placeholder = { Text("e.g. 90") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Popular Goals", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (preset in goalPresets.take(4)) {
                        FilterChip(
                            selected = title == preset,
                            onClick = {
                                title = preset
                                selectedCategory = preset
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title for your savings goal"
                        return@Button
                    }
                    val amt = targetText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid target amount"
                        return@Button
                    }
                    val days = targetDays.toIntOrNull() ?: 90
                    onConfirm(title.trim(), amt, days, selectedCategory)
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ContributeGoalDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("1000") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings to '${goal.title}'", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Current: ₹${"%,.0f".format(goal.currentAmount)} of ₹${"%,.0f".format(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Remaining to reach goal: ₹${"%,.0f".format(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Contribution Amount (₹)") },
                    leadingIcon = { Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("contribute_amount_input")
                )

                // Quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("500", "1000", "2000", "5000").forEach { quick ->
                        OutlinedButton(
                            onClick = { amountText = quick },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+₹$quick", fontSize = 11.sp)
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter an amount greater than 0"
                        return@Button
                    }
                    onConfirm(amt)
                },
                modifier = Modifier.testTag("confirm_contribute_button")
            ) {
                Text("Add to Savings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SmsSimulatorDialog(
    onDismiss: () -> Unit,
    onTestSms: (smsBody: String, sender: String) -> Unit
) {
    val sampleBankMessages = listOf(
        Pair(
            "AX-TEST",
            "UTR: ABC123456\nAmount: ₹500\nMerchant: ABC Store"
        ),
        Pair(
            "VK-HDFCBK",
            "Dear Customer, INR 450.00 debited from A/c XX4512 on 06-OCT-26 towards Swiggy UPI. Avl Bal: INR 18,240.00."
        ),
        Pair(
            "AX-SBIINB",
            "Txn of Rs. 2,500.00 with SBI Debit Card XX3311 done at HDFC ATM CASH WDL on 06-OCT-26. Avl Bal: Rs 12,000."
        ),
        Pair(
            "BZ-ICICIB",
            "Rs 1,299.00 spent on ICICI Bank Card ending 8012 at AMAZON INDIA on 05-OCT-26. Avl Limit: Rs 45,000."
        ),
        Pair(
            "VM-AXISBK",
            "Your A/C 9876 is credited with Rs 45,000.00 on 01-OCT-26 by Salary NEFT-INFY. Avl Bal Rs 52,100."
        ),
        Pair(
            "BW-PAYTMB",
            "Paid Rs.350 to Uber India using UPI ID user@paytm. Ref no 3482910. Avl Bal INR 4,120."
        )
    )

    var customBody by remember { mutableStateOf(sampleBankMessages[0].second) }
    var customSender by remember { mutableStateOf(sampleBankMessages[0].first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bank SMS Simulator 📲", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select an Indian Bank sample SMS or edit below to test automated transaction extraction without waiting for real SMS:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick presets
                sampleBankMessages.forEachIndexed { index, (sender, body) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                customSender = sender
                                customBody = body
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (customBody == body) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Sample #${index + 1} ($sender)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("SMS Sender") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Message Text") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTestSms(customBody, customSender)
                },
                modifier = Modifier.testTag("simulate_sms_button")
            ) {
                Text("Process SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CloudSyncDialog(
    deviceId: String,
    lastSyncFormatted: String,
    isOnline: Boolean = false,
    unsyncedCount: Int = 0,
    onDismiss: () -> Unit,
    onTriggerSync: () -> Unit,
    onExportJson: () -> Unit,
    onExportFile: () -> Unit,
    onPickImportFile: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var importText by remember { mutableStateOf("") }
    var showImportField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = IncomeGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Local Data & Backup", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Local Privacy status card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = IncomeGreenContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔒 100% Local On-Device Storage",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IncomeGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All transactions, SMS insights, and budgets are kept exclusively on this device in private Room SQLite storage. No data is ever sent to any remote server or cloud database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Device ID: $deviceId • $lastSyncFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Local Backup trigger
                Button(
                    onClick = onTriggerSync,
                    modifier = Modifier.fillMaxWidth().testTag("trigger_cloud_sync_button")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💾 Create Local Backup / Export Now")
                }

                Text(
                    text = "DATA EXPORT & RESTORE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Export to file / share
                OutlinedButton(
                    onClick = onExportFile,
                    modifier = Modifier.fillMaxWidth().testTag("export_file_button")
                ) {
                    Text("📁 Export Data to File / Share")
                }

                // Import from storage file
                OutlinedButton(
                    onClick = onPickImportFile,
                    modifier = Modifier.fillMaxWidth().testTag("import_file_button")
                ) {
                    Text("📥 Import Data from Storage (.json)")
                }

                // Copy JSON to clipboard
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Encrypted JSON to Clipboard")
                }

                if (!showImportField) {
                    TextButton(
                        onClick = { showImportField = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste JSON Text to Restore...")
                    }
                } else {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste JSON Backup") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImportJson(importText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("restore_backup_button"),
                        enabled = importText.isNotBlank()
                    ) {
                        Text("Restore Data")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun PinSetupDialog(
    currentHasPin: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (pin: String) -> Unit,
    onRemovePin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentHasPin) "Change 4-Digit PIN" else "Set 4-Digit PIN", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Secure Paisa Bachao with a 4-digit numeric PIN and biometric authentication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) pin = it.filter { c -> c.isDigit() }
                        errorMsg = null
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4) confirmPin = it.filter { c -> c.isDigit() }
                        errorMsg = null
                    },
                    label = { Text("Confirm 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_confirm_input")
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }

                if (currentHasPin) {
                    TextButton(
                        onClick = onRemovePin,
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                    ) {
                        Text("Remove PIN Protection")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4) {
                        errorMsg = "PIN must be exactly 4 digits"
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMsg = "PINs do not match"
                        return@Button
                    }
                    onSavePin(pin)
                },
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    decryptedSmsBody: String,
    onDismiss: () -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (transaction.type == "EXPENSE") "-₹${"%,.2f".format(transaction.amount)}" else "+₹${"%,.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (transaction.type == "EXPENSE") ExpenseRed else IncomeGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Category:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(transaction.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Account:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(transaction.accountOrBank, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Source:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (transaction.source == "SMS_AUTO") "Auto Bank SMS 📲" else if (transaction.source == "MANUAL_CASH") "Cash 💵" else "Manual Entry",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(transaction.timestamp)), style = MaterialTheme.typography.bodySmall)
                        }
                        if (transaction.notes.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Notes:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(transaction.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // If sourced from SMS, show encrypted raw SMS view
                if (transaction.source == "SMS_AUTO" && decryptedSmsBody.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Encrypted Bank SMS Audit (${transaction.smsSender})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = decryptedSmsBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDelete(transaction) },
                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
