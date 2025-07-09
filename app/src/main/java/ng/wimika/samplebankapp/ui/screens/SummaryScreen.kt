package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val flowState = MoneyGuardClientApp.accountProtectionFlowState

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
        ) {
            // Subtitle
            Text(
                text = "A summary of your subscription options.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = Color(0xFF6B6B6B),
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )
            // Accounts Covered
            Text(
                text = "Accounts Covered",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                flowState?.selectedAccountIds?.forEach { accountId ->
                    val account = flowState.allAccounts.find { it.id.toString() == accountId }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFB9A9F7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = account?.let { "${it.type} - ${it.number}" } ?: accountId,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = Color.Black
                        )
                    }
                }
            }
            // Amount Covered
            Text(
                text = "Amount Covered",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFB9A9F7),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = flowState?.amountToCover?.ifEmpty { "Not selected" } ?: "Not selected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.Black
                )
            }
            // Subscription Plan
            Text(
                text = "Subscription Plan",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFB9A9F7),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = flowState?.getSubscriptionPlanDisplay()?.ifEmpty { "Not selected" } ?: "Not selected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6)),
            ) {
                Text(
                    text = "Checkout",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                )
            }
        }
    }
} 