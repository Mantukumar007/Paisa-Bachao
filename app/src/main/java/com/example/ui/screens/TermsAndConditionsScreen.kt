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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms & Conditions",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("terms_back_btn")
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Terms of Service & Usage Agreement",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Please read these terms carefully before using Paisa Bachao.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "By using this application, you agree to these Terms & Conditions.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                TermsSectionCard(
                    number = "1",
                    title = "Purpose",
                    icon = Icons.Default.Description,
                    content = "This application is intended to help users manage and track their personal expenses and transactions. It is an expense-management tool and is not a bank, payment service, financial institution, or investment service."
                )
            }

            item {
                TermsSectionCard(
                    number = "2",
                    title = "Transaction Information",
                    icon = Icons.Default.Assignment,
                    content = "Transaction information may be detected from SMS messages when the user grants the required permission. The application attempts to identify transactions from available information, but parsing may occasionally be inaccurate. Users should verify important financial information independently."
                )
            }

            item {
                TermsSectionCard(
                    number = "3",
                    title = "No Financial Advice",
                    icon = Icons.Default.Warning,
                    content = "The application does not provide financial, investment, banking, tax, legal, or professional financial advice. Information displayed by the application should not be treated as professional advice."
                )
            }

            item {
                TermsSectionCard(
                    number = "4",
                    title = "Accuracy",
                    icon = Icons.Default.Info,
                    content = "The application may depend on SMS formats and transaction information provided by banks or payment services. The developer does not guarantee that every transaction will be detected, categorized, or interpreted correctly."
                )
            }

            item {
                TermsSectionCard(
                    number = "5",
                    title = "User Responsibility",
                    icon = Icons.Default.Security,
                    content = "The user is responsible for reviewing transaction records, correcting incorrect categories/details, maintaining device security, protecting access to the device, and keeping important financial records independently where necessary."
                )
            }

            item {
                TermsSectionCard(
                    number = "6",
                    title = "SMS Permission",
                    icon = Icons.Default.CheckCircle,
                    content = "SMS-based functionality requires the relevant Android permission. The user may deny or revoke the permission at any time. The application must respect the Android permission system."
                )
            }

            item {
                TermsSectionCard(
                    number = "7",
                    title = "Local Data",
                    icon = Icons.Default.Description,
                    content = "The application is designed to store transaction information locally. Uninstalling the application may permanently remove its private local database. The application does not guarantee recovery of data after uninstall, device reset, storage failure, or accidental deletion."
                )
            }

            item {
                TermsSectionCard(
                    number = "8",
                    title = "Availability",
                    icon = Icons.Default.Info,
                    content = "The developer does not guarantee that the application will always operate without errors or that every SMS/transaction will be detected correctly."
                )
            }

            item {
                TermsSectionCard(
                    number = "9",
                    title = "Third-Party Services",
                    icon = Icons.Default.Security,
                    content = "The application should not require a cloud database or remote service for its core transaction functionality. If any third-party service is added in the future, update the Privacy Policy and Terms & Conditions accordingly."
                )
            }

            item {
                TermsSectionCard(
                    number = "10",
                    title = "Updates",
                    icon = Icons.Default.Info,
                    content = "Features, functionality, UI, security mechanisms and supported Android versions may change through future updates."
                )
            }

            item {
                TermsSectionCard(
                    number = "11",
                    title = "Acceptance",
                    icon = Icons.Default.CheckCircle,
                    content = "By continuing to use the application, the user acknowledges that they have read and understood these Terms & Conditions."
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
fun TermsSectionCard(
    number: String,
    title: String,
    icon: ImageVector,
    content: String
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$number.",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}
