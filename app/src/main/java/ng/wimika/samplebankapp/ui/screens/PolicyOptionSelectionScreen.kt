package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
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
            CenterAlignedTopAppBar(
                title = { Text("Select Policy Option") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
                // Policy options list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(policyOptions) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption?.id == option.id,
                                onClick = { selectedOption = option }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(option.priceAndTerm.price),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "Term: ${option.priceAndTerm.term}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Auto-renew toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-renew Policy",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = autoRenew,
                        onCheckedChange = { autoRenew = it }
                    )
                }

                // Continue button
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
                        .padding(vertical = 16.dp),
                    enabled = selectedOption != null
                ) {
                    Text("Continue")
                }
            }
        }
    }
} 