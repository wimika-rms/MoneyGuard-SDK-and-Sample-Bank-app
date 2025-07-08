package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.CoverageLimit
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences
import ng.wimika.samplebankapp.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageLimitSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val token = preferenceManager?.getMoneyGuardToken()
    val flowState = MoneyGuardClientApp.accountProtectionFlowState
    
    var coverageLimits by remember { mutableStateOf(flowState?.allCoverageLimits ?: emptyList()) }
    var selectedLimitId by remember { mutableStateOf(flowState?.selectedCoverageLimit?.id) }
    var isLoading by remember { mutableStateOf(flowState?.allCoverageLimits?.isEmpty() != false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load coverage limits when screen is first loaded (only if not already loaded)
    LaunchedEffect(Unit) {
        if (flowState?.allCoverageLimits?.isEmpty() != false && sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getCoverageLimits(token)
                result.fold(
                    onSuccess = { response ->
                        coverageLimits = response.coverageLimits
                        flowState?.setAllCoverageLimits(response.coverageLimits)
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Failed to load coverage limits: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                Toast.makeText(
                    context,
                    "Error loading coverage limits: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (flowState?.allCoverageLimits?.isEmpty() == false) {
            // Already have coverage limits loaded, just update local state
            coverageLimits = flowState.allCoverageLimits
            isLoading = false
        } else {
            Toast.makeText(
                context,
                "Please login to view coverage limits",
                Toast.LENGTH_SHORT
            ).show()
            isLoading = false
        }
    }

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
                    text = "Amount to Cover",
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
                text = "Select the amount you want to cover for your accounts. This cover amount is for all your accounts.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = Color(0xFF6B6B6B),
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(coverageLimits) { limit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (selectedLimitId == limit.id) Color(0xFF8854F6) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(Color.White, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLimitId == limit.id,
                                onClick = { 
                                    selectedLimitId = limit.id 
                                    flowState?.setSelectedCoverageLimit(limit)
                                    flowState?.setAmountToCover(CurrencyFormatter.format(limit.limit))
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8854F6))
                            )
                            Text(
                                text = CurrencyFormatter.format(limit.limit),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                                color = Color.Black,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
                // Coverage Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF8854F6),
                            shape = RoundedCornerShape(12.dp),
                            // Dashed border effect
                            // Compose doesn't support dashed border natively, so we use a solid border for now
                        )
                        .background(Color(0xFFF8F6FF), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Coverage Notice",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Text(
                            text = "This coverage amount would be spread the accounts covered. Claims will be considered for sum of losses from the covered accounts within the coverage amount within the policy duration.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF6B6B6B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        // Selection is already saved to flow state on each change
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6)),
                    enabled = selectedLimitId != null
                ) {
                    Text(
                        text = "Next",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                    )
                }
            }
        }
    }
} 