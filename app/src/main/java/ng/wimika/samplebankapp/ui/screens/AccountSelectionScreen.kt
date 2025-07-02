package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.times
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val token = preferenceManager?.getMoneyGuardToken()

    var accounts by remember { mutableStateOf<List<BankAccount>>(emptyList()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load accounts when screen is first loaded
    LaunchedEffect(Unit) {
        if (sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101)
                result.fold(
                    onSuccess = { response ->
                        accounts = response.bankAccounts
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Failed to load accounts: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                Toast.makeText(
                    context,
                    "Error loading accounts: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Please login to view accounts",
                Toast.LENGTH_SHORT
            ).show()
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = -12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Accounts to Cover",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) // To balance the back arrow
            }
            // Subtitle
            Text(
                text = "Select the account you want to protect, you can protect more than one account.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = Color(0xFF6B6B6B),
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )
            // Select All
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                RadioButton(
                    selected = selectedAccounts.size == accounts.size && accounts.isNotEmpty(),
                    onClick = {
                        selectedAccounts = if (selectedAccounts.size == accounts.size) emptySet() else accounts.map { it.id.toString() }.toSet()
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8854F6))
                )
                Text(
                    text = "Select all",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = if (selectedAccounts.size == accounts.size && accounts.isNotEmpty()) Color(0xFF8854F6) else Color(0xFF6B6B6B),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            // Accounts list
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
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
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (selectedAccounts.contains(account.id.toString())) Color(0xFF8854F6) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(Color.White, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAccounts.contains(account.id.toString()),
                                onClick = {
                                    selectedAccounts = if (selectedAccounts.contains(account.id.toString())) {
                                        selectedAccounts - account.id.toString()
                                    } else {
                                        selectedAccounts + account.id.toString()
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8854F6))
                            )
                            Column(
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = account.type,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = Color.Black
                                )
                                Text(
                                    text = account.number,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Next button
            Button(
                onClick = {
                    // Save selected accounts to preferences
                    val currentPreferences = preferenceManager?.getMoneyGuardSetupPreferences()
                        ?: MoneyGuardSetupPreferences()
                    val updatedPreferences = currentPreferences.copy(
                        accountIds = selectedAccounts.toList()
                    )
                    preferenceManager?.saveMoneyGuardSetupPreferences(updatedPreferences)
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6)),
                enabled = selectedAccounts.isNotEmpty()
            ) {
                Text(
                    text = "Next",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}