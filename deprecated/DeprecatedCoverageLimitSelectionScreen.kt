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
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.CoverageLimit
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.moneyguardsdkclient.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageLimitSelectionScreen(
    moneyGuardPolicy: MoneyGuardPolicy,
    token: String,
    onBack: () -> Unit,
    onContinue: (Int) -> Unit
) {
    var coverageLimits by remember { mutableStateOf<List<CoverageLimit>>(emptyList()) }
    var selectedLimitId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        moneyGuardPolicy.getCoverageLimits(token).fold(
            onSuccess = { response ->
                coverageLimits = response.coverageLimits
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

                Button(
                    onClick = { selectedLimitId?.let { onContinue(it) } },
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