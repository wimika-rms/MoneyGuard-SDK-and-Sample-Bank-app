package ng.wimika.samplebankapp.ui.screens.claims

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimResponse
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.utils.CurrencyFormatter
import ng.wimika.samplebankapp.utils.DateUtils
import ng.wimika.samplebankapp.ui.theme.SabiBankColors
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimStatus
import java.util.Date

// Extension function to safely convert nullable string to ClaimStatus
fun String?.toClaimStatus(): ClaimStatus? {
    return try {
        this?.let { ClaimStatus.valueOf(it) }
    } catch (e: IllegalArgumentException) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsStatusFilter(
    selectedStatus: ClaimStatus?,
    onStatusSelected: (ClaimStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val statusOptions = listOf(
        null to "All Claims",
        ClaimStatus.Submitted to "Submitted",
        ClaimStatus.UnderReview to "Under Review",
        ClaimStatus.Verified to "Verified",
        ClaimStatus.Rejected to "Rejected",
        ClaimStatus.ProcessingPayment to "Processing Payment",
        ClaimStatus.PaymentSent to "Payment Sent",
        ClaimStatus.ReimbursementComplete to "Reimbursement Complete"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter by Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = statusOptions.find { it.first == selectedStatus }?.second ?: "All Claims",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown, 
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    statusOptions.forEach { (status, displayName) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onStatusSelected(status)
                                expanded = false
                            },
                            leadingIcon = if (selectedStatus == status) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsListScreen(
    onBackPressed: () -> Unit = {},
    onClaimClick: (claimId: Int) -> Unit = {},
    onSubmitNewClaim: () -> Unit = {}
) {
    // Get SDK services
    val moneyGuardClaim = MoneyGuardClientApp.sdkService?.claim()
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    
    // Internal state management
    var state by remember {
        mutableStateOf(
            ClaimsListState(
                claims = emptyList(),
                isLoading = false,
                errorMessage = null,
                status = null
            )
        )
    }

    // Event handler function
    fun onEvent(event: ClaimsListEvent) {
        when (event) {
            ClaimsListEvent.LoadClaims, ClaimsListEvent.RefreshClaims -> {
                state = state.copy(isLoading = true, errorMessage = null)
                
                val token = preferenceManager?.getMoneyGuardToken() ?: ""
                if (token.isBlank()) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "No authentication token found"
                    )
                    return
                }
                
                moneyGuardClaim?.getClaims(
                    token,
                    from = Date(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000),
                    to = Date(System.currentTimeMillis()),
                    bank = "",
                    claimStatus = state.status ?: ClaimStatus.Submitted,
                    onSuccess = { response ->
                        state = state.copy(
                            isLoading = false,
                            claims = response, // Assuming response is List<ClaimResponse>
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load claims"
                        )
                    }
                )
            }
            is ClaimsListEvent.FilterByStatus -> {
                state = state.copy(status = event.status)
                onEvent(ClaimsListEvent.LoadClaims) // Reload with new filter
            }
        }
    }

    // Load claims on first composition
    LaunchedEffect(Unit) {
        onEvent(ClaimsListEvent.LoadClaims)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Claims",
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
                actions = {
                    IconButton(onClick = { onEvent(ClaimsListEvent.RefreshClaims) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SabiBankColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSubmitNewClaim,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Submit New Claim",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Filter Dropdown
            ClaimsStatusFilter(
                selectedStatus = state.status,
                onStatusSelected = { status ->
                    onEvent(ClaimsListEvent.FilterByStatus(status))
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Show error message if any
                state.errorMessage?.let { error ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onEvent(ClaimsListEvent.RefreshClaims) }
                        ) {
                            Text("Retry")
                        }
                    }
                    return@Box
                }

                // Show loading indicator
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    return@Box
                }

                // Show empty state or claims list
                if (state.claims.isEmpty() && !state.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "No Claims",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.status != null) "No claims found for selected status" else "No claims found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.status != null) "Try changing the filter or submit your first claim" else "Submit your first claim using the + button",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Box
                }

                // Claims list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    item {
                        ClaimsSummaryCard(claims = state.claims)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Claims list
                    items(state.claims) { claim ->
                        ClaimListItem(
                            claim = claim,
                            onClick = { onClaimClick(claim.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClaimsSummaryCard(claims: List<ClaimResponse>) {
    val totalClaims = claims.size
    val totalAmount = claims.sumOf { it.lossAmount }
    val pendingClaims = claims.count { 
        val status = it.status.toClaimStatus()
        status == ClaimStatus.Submitted || status == ClaimStatus.UnderReview
    }
    val approvedClaims = claims.count { 
        val status = it.status.toClaimStatus()
        status == ClaimStatus.Verified || status == ClaimStatus.PaymentSent || status == ClaimStatus.ReimbursementComplete
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Claims Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total Claims",
                    value = totalClaims.toString(),
                    icon = Icons.Default.Receipt
                )
                SummaryItem(
                    label = "Pending",
                    value = pendingClaims.toString(),
                    icon = Icons.Default.Pending
                )
                SummaryItem(
                    label = "Approved",
                    value = approvedClaims.toString(),
                    icon = Icons.Default.CheckCircle
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = CurrencyFormatter.format(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ClaimListItem(
    claim: ClaimResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with claim ID and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Claim #${claim.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                claim.status.toClaimStatus()?.let { status ->
                    ClaimStatusChip(status = status)
                }
            }
            
            // Incident and amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = claim.natureOfIncident,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Account: ${claim.account}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SabiBankColors.TextSecondary
                    )
                }
                Text(
                    text = CurrencyFormatter.format(claim.lossAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Dates row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Loss Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = SabiBankColors.TextSecondary
                    )
                    Text(
                        text = claim.lossDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Submitted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SabiBankColors.TextSecondary
                    )
                    Text(
                        text = claim.reportDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Statement preview (first 100 characters)
            if (claim.statement?.isNotBlank() == true) {
                if (claim.statement!!.length > 100) {
                    "${claim.statement!!.take(100)}..."
                } else {
                    claim.statement
                }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = SabiBankColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
} 