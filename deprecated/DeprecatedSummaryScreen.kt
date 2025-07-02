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
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.PolicyOption
import ng.wimika.moneyguardsdkclient.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    selectedAccounts: List<BankAccount>,
    selectedPolicyOption: PolicyOption,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Summary") },
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Accounts Covered",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(selectedAccounts) { account ->
                    Text(
                        text = "${account.type} - ${account.number}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = "Amount Covered",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = CurrencyFormatter.format(selectedPolicyOption.coverage.coverageLimit.limit),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = "Subscription Plan",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "${CurrencyFormatter.format(selectedPolicyOption.priceAndTerm.price)}/${selectedPolicyOption.priceAndTerm.term}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Checkout")
            }
        }
    }
} 