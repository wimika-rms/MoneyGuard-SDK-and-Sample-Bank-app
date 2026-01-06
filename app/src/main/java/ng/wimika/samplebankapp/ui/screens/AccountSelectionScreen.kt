package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.samplebankapp.MoneyGuardClientApp

// Define Figma Colors
private val ColorMainPurple = Color(0xFF8046FA)
private val ColorTextDark = Color(0xFF3C3F4D)
private val ColorTextLight = Color(0xFF828D96)
private val ColorBorderUnselected = Color(0xFFE0E0E0)
private val ColorShadow = Color(0x1A8046FA)

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
    val flowState = MoneyGuardClientApp.accountProtectionFlowState

    var accounts by remember { mutableStateOf(flowState?.allAccounts ?: emptyList()) }
    var selectedAccounts by remember { mutableStateOf(flowState?.selectedAccountIds ?: emptySet()) }
    var isLoading by remember { mutableStateOf(flowState?.allAccounts?.isEmpty() != false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load accounts logic
    LaunchedEffect(Unit) {
        if (flowState?.allAccounts?.isEmpty() != false && sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101)
                result.fold(
                    onSuccess = { response ->
                        accounts = response.bankAccounts
                        flowState?.setAllAccounts(response.bankAccounts)
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message
                        isLoading = false
                        Toast.makeText(context, "Failed to load accounts: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                Toast.makeText(context, "Error loading accounts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else if (flowState?.allAccounts?.isEmpty() == false) {
            accounts = flowState.allAccounts
            isLoading = false
        } else {
            Toast.makeText(context, "Please login to view accounts", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            // BOTTOM BAR AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    // 1. Respect the device system navigation bar (gesture area)
                    .navigationBarsPadding()
                    // 2. Add horizontal padding to match the cards
                    .padding(horizontal = 20.dp)
                    // 3. Add the visual spacing from the design (approx 16dp from bottom safe area)
                    .padding(bottom = 16.dp, top = 16.dp)
            ) {
                Button(
                    onClick = { onContinue() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp) // UPDATED: Exact height from CSS
                        .testTag("account_selection_next_button"),
                    shape = RoundedCornerShape(49.dp), // UPDATED: Exact radius from CSS
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorMainPurple,
                        disabledContainerColor = ColorMainPurple.copy(alpha = 0.5f)
                    ),
                    enabled = selectedAccounts.isNotEmpty()
                ) {
                    Text(
                        text = "Next",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .offset(x = (-12).dp)
                        .testTag("account_selection_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorTextDark)
                }
                Text(
                    text = "Accounts to Cover",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    ),
                    color = ColorTextDark,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(36.dp))
            }

            // Subtitle
            Text(
                text = "Select the account you want to protect, you can protect more than one account.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = ColorTextLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            // Select All
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clickable {
                        val newSelection = if (selectedAccounts.size == accounts.size) emptySet() else accounts.map { it.id.toString() }.toSet()
                        selectedAccounts = newSelection
                        flowState?.setSelectedAccountIds(newSelection)
                    }
            ) {
                RadioButton(
                    selected = selectedAccounts.size == accounts.size && accounts.isNotEmpty(),
                    onClick = {
                        val newSelection = if (selectedAccounts.size == accounts.size) emptySet() else accounts.map { it.id.toString() }.toSet()
                        selectedAccounts = newSelection
                        flowState?.setSelectedAccountIds(newSelection)
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("account_selection_select_all_radio"),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ColorMainPurple,
                        unselectedColor = ColorTextLight
                    )
                )
                Text(
                    text = "Select all",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = if (selectedAccounts.size == accounts.size && accounts.isNotEmpty()) ColorMainPurple else ColorTextLight,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            // Accounts list
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorMainPurple)
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
                        val isSelected = selectedAccounts.contains(account.id.toString())

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 15.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    spotColor = ColorShadow,
                                    ambientColor = ColorShadow
                                )
                                .background(Color.White, shape = RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ColorMainPurple else ColorBorderUnselected,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val newSelection = if (isSelected) {
                                        selectedAccounts - account.id.toString()
                                    } else {
                                        selectedAccounts + account.id.toString()
                                    }
                                    selectedAccounts = newSelection
                                    flowState?.setSelectedAccountIds(newSelection)
                                }
                                .padding(vertical = 16.dp, horizontal = 16.dp)
                                .testTag("account_selection_item_${account.id}"),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("account_selection_radio_${account.id}"),
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ColorMainPurple,
                                    unselectedColor = ColorBorderUnselected
                                )
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text(
                                    text = account.type,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = ColorTextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = account.number,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = ColorTextLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}