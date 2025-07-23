package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    onProtectAccount: () -> Unit,
    onDownloadMoneyGuard: () -> Unit,
    onCheckDebitClick: () -> Unit = {},
    onEnrollTypingPattern: () -> Unit = {},
    onVerifyTypingPattern: () -> Unit = {},
    onNavigateToClaims: () -> Unit = {}
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

    Scaffold { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color(0xFFF5F6FA) // Light grey background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    userName = userFullName,
                    //isProtected = moneyguardStatus == MoneyGuardAppStatus.Active,
                    moneyguardStatus,
                    onProtectAccount = onProtectAccount,
                    onDownloadMoneyGuard = onDownloadMoneyGuard
                )
                Spacer(modifier = Modifier.height(24.dp))
                AccountCard()
                // Risk Score Card - only show when MoneyGuard is Active
                moneyguardStatus == MoneyGuardAppStatus.ValidPolicyAppNotInstalled
                if (moneyguardStatus == MoneyGuardAppStatus.Active) {
                    RiskScoreCard()
                }
                // Pager indicator from original UI
                //  PagerIndicator(pageCount = 4, currentPage = 0)
                Spacer(modifier = Modifier.height(24.dp))
                ActionsGrid(onCheckDebitClick = onCheckDebitClick,
                    onEnrollTypingPattern = onEnrollTypingPattern,
                    onVerifyTypingPattern = onVerifyTypingPattern,
                    onNavigateToClaims = onNavigateToClaims,
                    moneyguardStatus = moneyguardStatus)

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
}

@Composable
private fun DashboardHeader(userName: String, moneyGuardAppStatus: MoneyGuardAppStatus?, onProtectAccount: () -> Unit, onDownloadMoneyGuard: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
                if (moneyGuardAppStatus == MoneyGuardAppStatus.ValidPolicyAppNotInstalled) {
                    onDownloadMoneyGuard()
                }
                else if(moneyGuardAppStatus == MoneyGuardAppStatus.NoPolicyAppInstalled
                    || moneyGuardAppStatus == MoneyGuardAppStatus.InActive)
                {
                    // Clear the flow state when starting account protection
                    MoneyGuardClientApp.accountProtectionFlowState?.clearState()
                    onProtectAccount()
                }
                else if(moneyGuardAppStatus == MoneyGuardAppStatus.Active)
                {
                    MoneyGuardClientApp.sdkService?.utility()?.launchMoneyGuardApp();
                }

                // TODO: Handle "Launch MoneyGuard" action when protected
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if(moneyGuardAppStatus != null) {
                Text(
                    text = if (moneyGuardAppStatus == MoneyGuardAppStatus.Active) {
                        "Launch MoneyGuard"
                    } else {
                        "Protect Account"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else{
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            }
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
private fun ActionsGrid(onCheckDebitClick: () -> Unit = {},
                        onEnrollTypingPattern: () -> Unit = {},
                        onVerifyTypingPattern: () -> Unit = {},
                        onNavigateToClaims: () -> Unit = {},
                        moneyguardStatus: MoneyGuardAppStatus?) {
    val showTypingPatternActions = moneyguardStatus == MoneyGuardAppStatus.ValidPolicyAppNotInstalled ||
            moneyguardStatus == MoneyGuardAppStatus.Active

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Fixed icons to use core material library

            ActionCard(icon = Icons.Default.StarBorder, text = "Make\nTransfer",
                modifier = Modifier.weight(1f), onClick = onCheckDebitClick)

            // Conditionally show the "Enroll" button
            if (showTypingPatternActions) {
                ActionCard(
                    icon = Icons.Default.Security,
                    text = "Enroll\nTyping Pattern",
                    modifier = Modifier.weight(1f),
                    onClick = onEnrollTypingPattern
                )
            } else {
                // Add an empty placeholder to maintain grid alignment
                Box(modifier = Modifier.weight(1f))
            }

            // Conditionally show the "Verify" button
            if (showTypingPatternActions) {
                ActionCard(
                    icon = Icons.Default.Fingerprint,
                    text = "Verify\nTyping Pattern",
                    modifier = Modifier.weight(1f),
                    onClick = onVerifyTypingPattern
                )
            } else {
                // Add an empty placeholder to maintain grid alignment
                Box(modifier = Modifier.weight(1f))
            }
        }

        // Second row with Claims button
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(
                icon = Icons.Default.Receipt,
                text = "Claims",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToClaims
            )
            
            // Add empty placeholders to maintain grid alignment
            Box(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f))
        }

    }
}

@Composable
private fun ActionCard(icon: ImageVector, text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        onClick = onClick
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

@Composable
private fun RiskScoreCard() {
    val sdkService = MoneyGuardClientApp.sdkService
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val token = preferenceManager?.getMoneyGuardToken()
    var riskScore by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Continuously check for risk score at 3-second intervals until we get a score > 0
    LaunchedEffect(token) {
        if (sdkService != null && !token.isNullOrEmpty()) {
            while (true) {
                try {
                    val riskProfile = sdkService.riskProfile()?.getRiskProfile()
                    val currentRiskScore = riskProfile?.sumOf { it.score.value.toInt() } ?: 0
                    
                    if (currentRiskScore > 0) {
                        riskScore = currentRiskScore
                        isLoading = false
                        break // Exit the loop once we get a valid score
                    }
                    
                    // Wait 3 seconds before next check
                    kotlinx.coroutines.delay(3000)
                } catch (e: Exception) {
                    // On error, wait 3 seconds before retrying
                    kotlinx.coroutines.delay(3000)
                }
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFF97316))
                Text(
                    text = "Please launch Moneyguard",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
        return
    }
    if (riskScore == null || riskScore == 0) return

    // Determine the message based on the risk score
    val message = when {
        riskScore!! < 40 -> "Your risk score is very low, you are making it easy for cyber criminals to take your money."
        riskScore!! in 40..49 -> "Your risk score is low, take modules to improve it."
        riskScore!! in 50..59 -> "Your risk score is good, but it can be better."
        riskScore!! in 60..69 -> "Your risk score is good, but it can be better."
        riskScore!! >= 70 -> "Your risk score is looking good, take modules to get extra points."
        else -> "Your risk score is good but it can be better."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color.Transparent),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Texts
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Risk Score",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7B8794)
                )
            }
            // Right: Score box
            Card(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(width = 60.dp, height = 60.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = riskScore.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF2D2D),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(Color(0xFFFF2D2D).copy(alpha = 0.5f), shape = RoundedCornerShape(1.dp))
                    )
                    Text(
                        text = "100",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF2D2D),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}