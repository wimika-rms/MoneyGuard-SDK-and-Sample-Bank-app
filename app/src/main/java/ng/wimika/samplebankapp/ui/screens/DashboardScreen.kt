package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus
import ng.wimika.samplebankapp.MoneyGuardClientApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onProtectAccount: () -> Unit
) {
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val userFullName = preferenceManager?.getBankUserFullName() ?: "Enioluwa Oke"
    val sdkService = MoneyGuardClientApp.sdkService;
    // State for MoneyGuard status
    var moneyguardStatus by remember { mutableStateOf<MoneyGuardAppStatus?>(null) }
    // LaunchedEffect to handle the suspend function call
    LaunchedEffect(key1 = Unit) {
        preferenceManager?.let { pref ->
            val token = pref.getMoneyGuardToken()
            token?.let {
                moneyguardStatus = sdkService?.utility()?.checkMoneyguardStatus(it)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F6FA) // Light grey background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            DashboardHeader(
                userName = userFullName,
                isProtected = moneyguardStatus == MoneyGuardAppStatus.Active,
                onProtectAccount = onProtectAccount
            )
            Spacer(modifier = Modifier.height(24.dp))
            AccountCard()
            // Pager indicator from original UI
            PagerIndicator(pageCount = 4, currentPage = 0)
            Spacer(modifier = Modifier.height(24.dp))
            ActionsGrid()

            // Spacer to push the logout button to the bottom
            Spacer(Modifier.weight(1f))

            // Logout Button
            OutlinedButton(
                onClick = {
                    // Clear all preferences before logging out
                    preferenceManager?.clear()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp) // Add padding at the bottom of the screen
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFF97316))
            ) {
                Text(
                    text = "Logout",
                    color = Color(0xFFF97316),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(userName: String, isProtected: Boolean, onProtectAccount: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp), // Padding from the top of the screen
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = userName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { 
                if (!isProtected) {
                    onProtectAccount()
                }
                // TODO: Handle "Launch MoneyGuard" action when protected
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isProtected) {
                    "Launch MoneyGuard"
                } else {
                    "Protect Account"
                },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

    }
}

@Composable
private fun AccountCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // Restored original height for better layout
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF97316)) // Vibrant Orange
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Balance",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "N10,000,000",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.VisibilityOff, // Fixed Icon
                        contentDescription = "Hide Balance",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Bottom section with VISA logo and card number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VISA",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(".. / ..", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        Text(".... .... .... 3040", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { iteration ->
            val color = if (currentPage == iteration) Color(0xFFF97316) else Color.LightGray.copy(alpha = 0.5f)
            val size = if (currentPage == iteration) 10.dp else 8.dp
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(size)
            )
        }
    }
}

@Composable
private fun ActionsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Fixed icons to use core material library
            ActionCard(icon = Icons.Default.ReceiptLong, text = "Account\nStatement", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Default.StarBorder, text = "Pay bills", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Default.Refresh, text = "Recent\nTransfers", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(icon = Icons.Default.Print, text = "Print\nReceipts", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Default.StarBorder, text = "Make\nTransfer", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Default.AttachMoney, text = "Get Loan", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF7ED)), // Very light orange
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color(0xFFF97316)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}