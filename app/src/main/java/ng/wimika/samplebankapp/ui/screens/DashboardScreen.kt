package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import ng.wimika.samplebankapp.MoneyGuardClientApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit
) {
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val userFullName = preferenceManager?.getBankUserFullName() ?: "Enioluwa Oke"
    // The original `isMoneyguardEnabled` logic is preserved here but the UI is built
    // to match the "not protected" state from the screenshot.
   //  val isMoneyguardEnabled = preferenceManager?.isMoneyguardEnabled() ?: false

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F6FA) // Light grey background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp) // Add bottom padding for the logout button
        ) {
            DashboardHeader(userName = userFullName)
            Spacer(modifier = Modifier.height(24.dp))
            AccountCard()

            Spacer(modifier = Modifier.height(24.dp)) // Add spacing between cards and grid

            Spacer(modifier = Modifier.height(24.dp))
            ActionsGrid()
        }
    }
}

@Composable
private fun DashboardHeader(userName: String) {
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val userFullName = preferenceManager?.getBankUserFullName() ?: "Enioluwa Oke"
    // The original `isMoneyguardEnabled` logic is preserved here but the UI is built
    // to match the "not protected" state from the screenshot.
      val isMoneyguardEnabled = preferenceManager?.isMoneyguardEnabled() ?: false

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
            onClick = { /* TODO: Handle "Protect account" action */ },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isMoneyguardEnabled) {
                    "Launch MoneyGuard"
                } else {
                    "Protect your account now"
                },
                color = Color.White,
                fontSize = 8.sp,
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
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF97316)) // Vibrant Orange
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The darker orange curve can be added here as a background element if needed
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
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = "Hide Balance",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Bottom section with VISA logo and card number


                }
            }
        }
    }




@Composable
private fun ActionsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(icon = Icons.Outlined.Article, text = "Account\nStatement", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Outlined.StarOutline, text = "Pay bills", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Outlined.History, text = "Recent\nTransfers", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(icon = Icons.Outlined.Print, text = "Print\nReceipts", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Outlined.StarOutline, text = "Make\nTransfer", modifier = Modifier.weight(1f))
            ActionCard(icon = Icons.Outlined.MonetizationOn, text = "Get Loan", modifier = Modifier.weight(1f))
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