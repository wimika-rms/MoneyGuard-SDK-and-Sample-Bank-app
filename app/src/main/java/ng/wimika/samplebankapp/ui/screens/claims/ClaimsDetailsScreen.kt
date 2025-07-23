package ng.wimika.samplebankapp.ui.screens.claims


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimStatus
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.utils.CurrencyFormatter
import ng.wimika.samplebankapp.utils.DateUtils
import ng.wimika.samplebankapp.ui.theme.SabiBankColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsDetailsScreen(
    claimId: Int,
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    val moneyGuardClaim = MoneyGuardClientApp.sdkService?.claim()
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    
    var detailsState by remember {
        mutableStateOf(
            ClaimsDetailsState(
                claim = null,
                isLoading = true,
                errorMessage = null
            )
        )
    }
    
    LaunchedEffect(claimId) {
        val token = preferenceManager?.getMoneyGuardToken() ?: ""
        if (token.isBlank()) {
            detailsState = detailsState.copy(
                isLoading = false,
                errorMessage = "No authentication token found"
            )
            return@LaunchedEffect
        }
        
        detailsState = detailsState.copy(isLoading = true, errorMessage = null)
        
        moneyGuardClaim?.getClaim(
            sessionToken = token,
            claimId = claimId,
            onSuccess = { response ->
                detailsState = detailsState.copy(
                    isLoading = false,
                    claim = response,
                    errorMessage = null
                )
            },
            onFailure = { error ->
                detailsState = detailsState.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load claim details"
                )
            }
        )
    }

    val currentClaim = detailsState.claim

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Claim Details",
                        color = SabiBankColors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SabiBankColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (detailsState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        detailsState.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = {
                            // Retry loading
                            val token = preferenceManager?.getMoneyGuardToken() ?: ""
                            if (token.isNotBlank()) {
                                detailsState = detailsState.copy(isLoading = true, errorMessage = null)
                                moneyGuardClaim?.getClaim(
                                    sessionToken = token,
                                    claimId = claimId,
                                    onSuccess = { response ->
                                        detailsState = detailsState.copy(
                                            isLoading = false,
                                            claim = response,
                                            errorMessage = null
                                        )
                                    },
                                    onFailure = { error ->
                                        detailsState = detailsState.copy(
                                            isLoading = false,
                                            errorMessage = error.message ?: "Failed to load claim details"
                                        )
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }
        
        if (currentClaim == null) {
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claim #${claimId}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        currentClaim.status?.let { ClaimStatusChip(status = ClaimStatus.valueOf(it)) }
                    }

                    Text(
                        text = CurrencyFormatter.format(currentClaim.lossAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )


                    Text(
                        text = currentClaim.natureOfIncident,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Account Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Account Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )


                    currentClaim.account?.let {
                        DetailRow(
                            label = "Account Number",
                            value = it,
                            icon = Icons.Default.AccountBalance
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Incident Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DetailRow(
                        label = "Incident Type",
                        value = currentClaim.natureOfIncident,
                        icon = Icons.Default.Report
                    )

                    DetailRow(
                        label = "Loss Amount",
                        value = CurrencyFormatter.format(currentClaim.lossAmount),
                        icon = Icons.Default.AttachMoney
                    )

                    DetailRow(
                        label = "Loss Date",
                        value = DateUtils.formatDate(currentClaim.lossDate),
                        icon = Icons.Default.CalendarToday
                    )

                    DetailRow(
                        label = "Submission Date",
                        value = DateUtils.formatDate(currentClaim.reportDate),
                        icon = Icons.Default.Schedule
                    )
                }
            }

            // Statement
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Statement",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Statement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        currentClaim?.statement?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SabiBankColors.TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}