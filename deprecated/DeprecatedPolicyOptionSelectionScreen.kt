package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.PolicyOption
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.moneyguardsdkclient.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyOptionSelectionScreen(
    moneyGuardPolicy: MoneyGuardPolicy,
    token: String,
    coverageLimitId: Int,
    onBack: () -> Unit,
    onContinue: (PolicyOption) -> Unit
) {
    var policyOptions by remember { mutableStateOf<List<PolicyOption>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<PolicyOption?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coverageLimitId) {
        moneyGuardPolicy.getPolicyOptions(token, coverageLimitId).fold(
            onSuccess = { response ->
                policyOptions = response.policyOptions
                isLoading = false
            },
            onFailure = { exception ->
                error = exception.message
                isLoading = false
            }
        )
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

                Button(
                    onClick = { selectedOption?.let { onContinue(it) } },
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