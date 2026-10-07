package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("privacy_policy_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Privacy Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreenContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🔒 Your transaction data stays on this device",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                            Text(
                                text = "100% Local processing • No cloud databases • No remote tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "This application is designed with a local-first approach. Transaction and expense information processed by the application is intended to remain on the user's device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                PolicySectionCard(
                    title = "Information the App May Access",
                    icon = Icons.Default.Visibility,
                    bullets = listOf(
                        "Bank/payment-related SMS messages after the user grants the required permission.",
                        "Transaction amount.",
                        "Merchant information.",
                        "UTR / transaction ID / reference number.",
                        "Transaction date and time.",
                        "Expense category.",
                        "User-created expense information.",
                        "Local app settings."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "How Data Is Used",
                    icon = Icons.Default.CheckCircle,
                    bullets = listOf(
                        "Detect transactions.",
                        "Create expense records.",
                        "Categorize expenses.",
                        "Display transaction history.",
                        "Detect duplicate transactions.",
                        "Provide local expense-management features."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "Local Processing",
                    icon = Icons.Default.Storage,
                    paragraphs = listOf(
                        "Transaction and SMS processing is performed locally on the device where supported.",
                        "The app does not intentionally upload transaction information, SMS content, UTRs, merchant details or financial records to a cloud database or remote server."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "Data Sharing",
                    icon = Icons.Default.Block,
                    paragraphs = listOf(
                        "The app does not sell or share transaction information with advertisers or third parties.",
                        "We do not implement third-party analytics that collect financial transaction information."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "Data Storage",
                    icon = Icons.Default.Storage,
                    paragraphs = listOf(
                        "Transaction data is stored locally using the app's local database/storage.",
                        "Users should understand that uninstalling the app may remove app-private local data. Reinstall recovery can only rebuild transactions from SMS that are still available and accessible after permissions are granted."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "SMS Permission",
                    icon = Icons.Default.Message,
                    paragraphs = listOf(
                        "SMS access is used only for the transaction-detection functionality that requires it.",
                        "The app requests permission transparently and explains why the permission is needed before requesting it.",
                        "If permission is denied, SMS-based transaction detection will not function, but manual cash and offline expense tracking features continue to operate normally."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "Security",
                    icon = Icons.Default.Security,
                    paragraphs = listOf(
                        "Reasonable technical measures are used to protect locally stored information, including local at-rest encryption and optional biometric / PIN access controls.",
                        "The app does not expose sensitive financial information through logs, debug output, public endpoints, or unnecessary external services.",
                        "No absolute security guarantee is claimed."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "User Control",
                    icon = Icons.Default.Info,
                    bullets = listOf(
                        "Deny or revoke SMS permission at any time in system settings.",
                        "Remove individual transactions from the app.",
                        "Clear local application data through the app/device settings.",
                        "Uninstall the application at any time."
                    )
                )
            }

            item {
                PolicySectionCard(
                    title = "Changes to Privacy Policy",
                    icon = Icons.Default.Info,
                    paragraphs = listOf(
                        "If the application's data practices change, this Privacy Policy will be updated accordingly and appropriate notice will be provided."
                    )
                )
            }

            // Footer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Built with ❤️ by Mantu Kumar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://mantukumar-portfolio.vercel.app/?utm_source=chatgpt.com")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PolicySectionCard(
    title: String,
    icon: ImageVector,
    paragraphs: List<String> = emptyList(),
    bullets: List<String> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            for (p in paragraphs) {
                Text(
                    text = p,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }

            for (b in bullets) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = b,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
