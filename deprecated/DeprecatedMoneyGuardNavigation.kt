package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.PolicyOption
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.moneyguardsdkclient.ui.LocalToken

sealed class MoneyGuardScreen(val route: String) {
    object AccountSelection : MoneyGuardScreen("account_selection")
    object CoverageLimitSelection : MoneyGuardScreen("coverage_limit_selection")
    object PolicyOptionSelection : MoneyGuardScreen("policy_option_selection")
    object Summary : MoneyGuardScreen("summary")
    object Checkout : MoneyGuardScreen("checkout")
}

@Composable
fun MoneyGuardNavigation(
    navController: NavHostController = rememberNavController(),
    moneyGuardPolicy: MoneyGuardPolicy,
    token: String,
    onBack: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    var selectedAccounts: List<BankAccount> by remember { mutableStateOf(emptyList()) }
    var selectedPolicyOption: PolicyOption? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    //token = LocalToken.current ?: ""

    NavHost(
        navController = navController,
        startDestination = MoneyGuardScreen.AccountSelection.route
    ) {
        composable(MoneyGuardScreen.AccountSelection.route) {
            AccountSelectionScreen(
                moneyGuardPolicy = moneyGuardPolicy,
                token = token,
                onBack = onBack,
                onContinue = { accounts ->
                    selectedAccounts = accounts
                    navController.navigate(MoneyGuardScreen.CoverageLimitSelection.route)
                }
            )
        }

        composable(MoneyGuardScreen.CoverageLimitSelection.route) {
            CoverageLimitSelectionScreen(
                moneyGuardPolicy = moneyGuardPolicy,
                token = token,
                onBack = { navController.popBackStack() },
                onContinue = { coverageLimitId ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "coverageLimitId",
                        coverageLimitId
                    )
                    navController.navigate(MoneyGuardScreen.PolicyOptionSelection.route)
                }
            )
        }

        composable(MoneyGuardScreen.PolicyOptionSelection.route) {
            val coverageLimitId = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Int>("coverageLimitId")
                ?: return@composable

            PolicyOptionSelectionScreen(
                moneyGuardPolicy = moneyGuardPolicy,
                token = token,
                coverageLimitId = coverageLimitId,
                onBack = { navController.popBackStack() },
                onContinue = { policyOption ->
                    selectedPolicyOption = policyOption
                    navController.navigate(MoneyGuardScreen.Summary.route)
                }
            )
        }

        composable(MoneyGuardScreen.Summary.route) {
            if (selectedAccounts.isEmpty() || selectedPolicyOption == null) {
                navController.popBackStack()
                return@composable
            }

            SummaryScreen(
                selectedAccounts = selectedAccounts,
                selectedPolicyOption = selectedPolicyOption!!,
                onBack = { navController.popBackStack() },
                onCheckout = {
                    navController.navigate(MoneyGuardScreen.Checkout.route)
                }
            )
        }

        composable(MoneyGuardScreen.Checkout.route) {
            val viewModel: MoneyGuardViewModel = viewModel(
                factory = MoneyGuardViewModelFactory(moneyGuardPolicy, token)
            )
            val isSuccess by viewModel.isSuccess.collectAsState()
            val successMessage by viewModel.successMessage.collectAsState()
            val error by viewModel.error.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                CheckoutScreen(
                    moneyGuardPolicy = moneyGuardPolicy,
                    token = token,
                    onBack = { navController.popBackStack() },
                    onProceed = { debitAccountId, autoRenew ->
                        if (selectedPolicyOption != null) {
                            viewModel.createPolicy(
                                policyOptionId = selectedPolicyOption!!.id.toString(),
                                coveredAccountIds = selectedAccounts.map { it.id.toString() },
                                debitAccountId = debitAccountId,
                                autoRenew = autoRenew
                            )
                        }
                    }
                )

                if (isSuccess) {
                    AlertDialog(
                        onDismissRequest = { /* Dialog cannot be dismissed */ },
                        title = { Text("Success") },
                        text = { Text(successMessage ?: "Policy created successfully!") },
                        confirmButton = {
                            Button(
                                onClick = { onGoToDashboard() }
                            ) {
                                Text("Go to Dashboard")
                            }
                        }
                    )
                }

                error?.let { errorMessage ->
                    //hack, fix later
                    if (errorMessage.contains("Policy created successfully"))
                    {
                        AlertDialog(
                            onDismissRequest = { /* Dialog cannot be dismissed */ },
                            title = { Text("Success") },
                            text = { Text(successMessage ?: "Policy created successfully!") },
                            confirmButton = {
                                Button(
                                    onClick = { onGoToDashboard() }
                                ) {
                                    Text("Go to Dashboard")
                                }
                            }
                        )
                    }
                    else {
                        LaunchedEffect(errorMessage) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
} 