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
    
    var coverageLimits by remember { mutableStateOf<List<CoverageLimit>>(emptyList()) }
    var selectedLimitId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load coverage limits when screen is first loaded
    LaunchedEffect(Unit) {
        if (sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getCoverageLimits(token)
                result.fold(
                    onSuccess = { response ->
                        coverageLimits = response.coverageLimits
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
            CenterAlignedTopAppBar(
                title = { Text("Amount to Cover") },
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
                .padding(horizontal = 16.dp, vertical = 54.dp)
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
                // Coverage limits list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(coverageLimits) { limit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLimitId == limit.id,
                                onClick = { selectedLimitId = limit.id }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(limit.limit),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                // Continue button
                Button(
                    onClick = { 
                        selectedLimitId?.let { limitId ->
                            val selectedLimit = coverageLimits.find { it.id == limitId }
                            val currentPreferences = preferenceManager?.getMoneyGuardSetupPreferences() 
                                ?: MoneyGuardSetupPreferences()
                            val updatedPreferences = currentPreferences.copy(
                                coverageLimitId = limitId.toString(),
                                amountToCover = selectedLimit?.let { CurrencyFormatter.format(it.limit) } ?: ""
                            )
                            preferenceManager?.saveMoneyGuardSetupPreferences(updatedPreferences)
                            onContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = selectedLimitId != null
                ) {
                    Text("Continue")
                }
            }
        }
    }
} 