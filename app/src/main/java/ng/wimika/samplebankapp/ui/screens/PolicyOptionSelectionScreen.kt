package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.PolicyOption
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences
import ng.wimika.samplebankapp.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyOptionSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val token = preferenceManager?.getMoneyGuardToken()
    val currentPreferences = preferenceManager?.getMoneyGuardSetupPreferences()
    val coverageLimitId = currentPreferences?.coverageLimitId?.toIntOrNull()
    
    var policyOptions by remember { mutableStateOf<List<PolicyOption>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<PolicyOption?>(null) }
    var autoRenew by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load policy options when screen is first loaded
    LaunchedEffect(Unit) {
        if (sdkService != null && !token.isNullOrEmpty() && coverageLimitId != null) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getPolicyOptions(token, coverageLimitId)
                result.fold(
                    onSuccess = { response ->
                        policyOptions = response.policyOptions
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Failed to load policy options: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                Toast.makeText(
                    context,
                    "Error loading policy options: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Please complete previous steps to view policy options",
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
                    text = "Choose your Plan",
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
                text = "Select the subscription plan you want.",
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
                    items(policyOptions) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (selectedOption?.id == option.id) Color(0xFF8854F6) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(Color.White, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption?.id == option.id,
                                onClick = { selectedOption = option },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8854F6))
                            )
                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = option.priceAndTerm.term,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                    color = Color.Black
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = CurrencyFormatter.format(option.priceAndTerm.price),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                        color = Color(0xFF8854F6)
                                    )
//                                    if (option.priceAndTerm.savingsPercent > 0) {
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                        Text(
//                                            text = "Save ${option.priceAndTerm.savingsPercent}%",
//                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
//                                            color = Color(0xFF8854F6)
//                                        )
//                                    }
                                }
                            }
                        }
                    }
                }
                // Auto-renew box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF8854F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Color(0xFFF8F6FF), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto renew",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                            Text(
                                text = "The best way to stay vigilant and enjoy our service without interruptions is auto-renewal. Turn it on and have peace of mind!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = Color(0xFF6B6B6B)
                            )
                        }
                        Switch(
                            checked = autoRenew,
                            onCheckedChange = { autoRenew = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF8854F6))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        selectedOption?.let { option ->
                            val currentPrefs = preferenceManager?.getMoneyGuardSetupPreferences() 
                                ?: MoneyGuardSetupPreferences()
                            val updatedPreferences = currentPrefs.copy(
                                policyOptionId = option.id.toString(),
                                subscriptionPlan = "${CurrencyFormatter.format(option.priceAndTerm.price)}/${option.priceAndTerm.term}",
                                autoRenew = autoRenew
                            )
                            preferenceManager?.saveMoneyGuardSetupPreferences(updatedPreferences)
                            onContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6)),
                    enabled = selectedOption != null
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