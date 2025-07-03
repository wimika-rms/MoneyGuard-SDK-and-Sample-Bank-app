package ng.wimika.moneyguardsdkclient.ui.features.dashboard

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import ng.wimika.moneyguardsdkclient.MoneyGuardClientApp
import ng.wimika.moneyguardsdkclient.LocalMoneyGuardUtility
import ng.wimika.moneyguardsdkclient.ui.LocalToken
import ng.wimika.moneyguardsdkclient.ui.features.dashboard.widgets.AccountDetailsCard
import ng.wimika.moneyguardsdkclient.ui.features.dashboard.widgets.QuickActionsCard
import ng.wimika.moneyguardsdkclient.ui.features.login.LoginViewModel
import ng.wimika.moneyguardsdkclient.utils.PermissionUtils
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardUtility
import ng.wimika.moneyguard_sdk.services.risk_profile.MoneyGuardRiskProfile
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.moneyguardsdkclient.LocalMoneyGuardRiskProfile

@Serializable
object Dashboard

@Composable
fun DashboardDestination(
    viewModel: LoginViewModel = viewModel(),
    onUtilitiesClick: () -> Unit,
    onEnableMoneyGuard: () -> Unit,
    onDebitCheckClick: () -> Unit,
    onClaimClick: () -> Unit,
    onTypingProfileClick: () -> Unit,
    onLogout: () -> Unit,
    token: String = ""
) {
    val context = LocalContext.current
    
    Log.d("TokenDebug-Dashboard", "Received token: $token")
    
    LaunchedEffect(token) {
        Log.d("TokenDebug-Dashboard", "LaunchedEffect triggered with token: $token")
        if (token.isNotEmpty()) {
            Log.d("TokenDebug-Dashboard", "Saving token to preferences")
            MoneyGuardClientApp.preferenceManager?.saveMoneyGuardToken(token)
        }
    }

    DashboardScreen(
        onUtilitiesClick = onUtilitiesClick,
        onDebitCheckClick = onDebitCheckClick,
        onLogout = {
            viewModel.logOut()
            onLogout()
        },
        onEnableMoneyGuard = onEnableMoneyGuard,
        onClaimClick = onClaimClick,
        onTypingProfileClick = onTypingProfileClick
    )
}

@Composable
fun DashboardScreen(
    onUtilitiesClick: () -> Unit,
    onEnableMoneyGuard: () -> Unit,
    onDebitCheckClick: () -> Unit,
    onClaimClick: () -> Unit,
    onTypingProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sdkUtils: MoneyGuardUtility? = LocalMoneyGuardUtility.current
    val sdkRiskProfile: MoneyGuardRiskProfile? = LocalMoneyGuardRiskProfile.current
    val token = LocalToken.current
    var moneyGuardStatus by remember { mutableStateOf<MoneyGuardAppStatus?>(null) }
    var riskScore by remember { mutableStateOf(0) }
    
    var hasLocationPermissions by remember {
        mutableStateOf(
            PermissionUtils.isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    && PermissionUtils.isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Check MoneyGuard status and compute risk score when screen loads
    LaunchedEffect(Unit) {
        if (sdkUtils != null && !token.isNullOrEmpty()) {
            moneyGuardStatus = sdkUtils.checkMoneyguardStatus(token)
            
            // Compute risk score if MoneyGuard is active
            if (moneyGuardStatus == MoneyGuardAppStatus.Active && sdkRiskProfile != null) {
                try {
                    val riskProfile = sdkRiskProfile.getRiskProfile()
                    val totalScore = riskProfile.sumOf { it.score.value.toInt() }
                    riskScore = totalScore
                } catch (e: Exception) {
                    // Handle any errors, keep riskScore as 0
                    android.util.Log.e("DashboardScreen", "Error getting risk profile: ${e.message}")
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermissions = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermissions) {
            onDebitCheckClick()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )

                Button(
                    onClick = onLogout,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout"
                    )
                }
            }

            AccountDetailsCard()

            // Risk Score Card - only show when MoneyGuard is Active
            if (moneyGuardStatus == MoneyGuardAppStatus.Active) {
                RiskScoreCard(riskScore = riskScore)
            }

            Box(modifier = Modifier.height(16.dp))

            QuickActionsCard(
                title = "Quick Actions",
                onUtilityClick = onUtilitiesClick,
                onDebitCheckClick = {
                    if (hasLocationPermissions) {
                        onDebitCheckClick()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onClaimsClick = onClaimClick,
                onTypingProfileClick = onTypingProfileClick,
                enableMoneyGuard = onEnableMoneyGuard
            )
        }
    }
}

@Composable
fun RiskScoreCard(riskScore: Int) {
    // Determine the message based on the risk score
    val message = when {
        riskScore < 40 -> "Your risk score is very low, you are making it easy for cyber criminals to take your money."
        riskScore in 40..49 -> "Your risk score is low, take modules to improve it"
        riskScore in 50..59 -> "Your risk score is good, but it can be better."
        riskScore in 60..69 -> "Your risk score is good, but it can be better."
        riskScore >= 70 -> "Your risk score is looking good, take modules to get extra points."
        else -> "Your risk score is good but it can be better"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text content on the left
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Risk Score",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Score card on the right
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = riskScore.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        // Horizontal divider
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(1.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(0.5.dp)
                                )
                        )
                        
                        Text(
                            text = "100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DashboardScreenPreview() {
    DashboardScreen(
        onDebitCheckClick = {},
        onUtilitiesClick = {},
        onLogout = {},
        onEnableMoneyGuard = {},
        onClaimClick = {},
        onTypingProfileClick = {}
    )
}