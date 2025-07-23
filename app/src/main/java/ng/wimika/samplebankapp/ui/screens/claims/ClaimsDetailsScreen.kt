package ng.wimika.samplebankapp.ui.screens.claims

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimStatus
import ng.wimika.samplebankapp.R
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.utils.CurrencyFormatter
import ng.wimika.samplebankapp.utils.DateUtils
import ng.wimika.samplebankapp.utils.FileUtils
import ng.wimika.samplebankapp.ui.theme.SabiBankColors
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsDetailsScreen(
    claimId: Int,
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Get SDK services
    val moneyGuardClaim = ng.wimika.samplebankapp.MoneyGuardClientApp.sdkService?.claim()
    val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
    
    // State for loading detailed claim information
    var detailsState by remember {
        mutableStateOf(
            ClaimsDetailsState(
                claim = null, // Start with no claim data
                isLoading = true, // Start loading immediately
                errorMessage = null
            )
        )
    }
    
    // Load detailed claim information on composition
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
        // Show loading indicator
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
        
        // Show error message
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
        
        // Show main content only when we have claim data
        if (currentClaim == null) {
            // This shouldn't happen if loading and error states are handled correctly,
            // but adding as a safety net
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
            // Claim Header Card
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


                    currentClaim?.natureOfIncident?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
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


                    currentClaim?.account?.let {
                        DetailRow(
                            label = "Account Number",
                            value = it,
                            icon = Icons.Default.AccountBalance
                        )
                    }
                }
            }

            // Incident Details
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
                    
                    currentClaim?.natureOfIncident?.let {
                        DetailRow(
                            label = "Incident Type",
                            value = it,
                            icon = Icons.Default.Report
                        )
                    }
                    
                    currentClaim?.lossAmount?.let {
                        DetailRow(
                            label = "Loss Amount",
                            value = CurrencyFormatter.format(it),
                            icon = Icons.Default.AttachMoney
                        )
                    }
                    
                    currentClaim?.lossDate?.let {
                        DetailRow(
                            label = "Loss Date",
                            value = it/*DateUtils.formatDate(it)*/,
                            icon = Icons.Default.CalendarToday
                        )
                    }
                    
                    currentClaim?.reportDate?.let {
                        DetailRow(
                            label = "Submission Date",
                            value = it/*DateUtils.formatDate(it)*/,
                            icon = Icons.Default.Schedule
                        )
                    }
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

            // Attachments
//            currentClaim?.attachments?.let { attachments ->
//                if (attachments.isNotEmpty()) {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surface
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(16.dp),
//                            verticalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Attachment,
//                                    contentDescription = "Attachments",
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                                Text(
//                                    text = "Attachments (${attachments.size})",
//                                    style = MaterialTheme.typography.titleMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    color = MaterialTheme.colorScheme.onSurface
//                                )
//                            }
//
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .horizontalScroll(rememberScrollState()),
//                                horizontalArrangement = Arrangement.spacedBy(12.dp)
//                            ) {
//                                attachments.forEachIndexed { index, uri ->
//                                    Card(
//                                        modifier = Modifier
//                                            .size(120.dp),
//                                        shape = RoundedCornerShape(12.dp),
//                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                                    ) {
//                                        val isImage = FileUtils.isFileAnImage(context, uri)
//
//                                        if (isImage) {
//                                            AsyncImage(
//                                                model = ImageRequest.Builder(context)
//                                                    .data(uri)
//                                                    .crossfade(true)
//                                                    .build(),
//                                                contentDescription = "Attachment $index",
//                                                modifier = Modifier
//                                                    .fillMaxSize()
//                                                    .clip(RoundedCornerShape(12.dp)),
//                                                contentScale = ContentScale.Crop
//                                            )
//                                        } else {
//                                            Box(
//                                                modifier = Modifier
//                                                    .fillMaxSize()
//                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
//                                                contentAlignment = Alignment.Center
//                                            ) {
//                                                Column(
//                                                    horizontalAlignment = Alignment.CenterHorizontally,
//                                                    verticalArrangement = Arrangement.Center
//                                                ) {
//                                                    Icon(
//                                                        painter = when {
//                                                            FileUtils.isFileAVideo(context, uri) -> painterResource(id = R.drawable.ic_video)
//                                                            FileUtils.isFileAnAudio(context, uri) -> painterResource(id = R.drawable.ic_sound)
//                                                            else -> painterResource(id = R.drawable.ic_file)
//                                                        },
//                                                        contentDescription = "File type",
//                                                        modifier = Modifier.size(40.dp),
//                                                        tint = SabiBankColors.Gray600
//                                                    )
//                                                    Spacer(modifier = Modifier.height(8.dp))
//                                                    Text(
//                                                        text = when {
//                                                            FileUtils.isFileAVideo(context, uri) -> "Video"
//                                                            FileUtils.isFileAnAudio(context, uri) -> "Audio"
//                                                            else -> "Document"
//                                                        },
//                                                        style = MaterialTheme.typography.bodySmall,
//                                                        color = SabiBankColors.Gray600
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Status Timeline
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surface
//                )
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Timeline,
//                            contentDescription = "Timeline",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                        Text(
//                            text = "Status Timeline",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//
//                    currentClaim?.status?.let { status ->
//                        StatusTimelineItem(
//                            title = "Claim Submitted",
//                            date = currentClaim?.submissionDate,
//                            isCompleted = true,
//                            isActive = status == ClaimStatus.PENDING
//                        )
//
//                        StatusTimelineItem(
//                            title = "Under Review",
//                            date = if (status != ClaimStatus.PENDING) currentClaim?.submissionDate else null,
//                            isCompleted = status != ClaimStatus.PENDING,
//                            isActive = status == ClaimStatus.UNDER_REVIEW
//                        )
//
//                        when (status) {
//                            ClaimStatus.APPROVED, ClaimStatus.PAID -> {
//                                StatusTimelineItem(
//                                    title = "Approved",
//                                    date = currentClaim?.submissionDate, // In real app, this would be separate date
//                                    isCompleted = true,
//                                    isActive = status == ClaimStatus.APPROVED
//                                )
//
//                                if (status == ClaimStatus.PAID) {
//                                    StatusTimelineItem(
//                                        title = "Payment Processed",
//                                        date = currentClaim?.submissionDate, // In real app, this would be separate date
//                                        isCompleted = true,
//                                        isActive = true
//                                    )
//                                }
//                            }
//                            ClaimStatus.REJECTED -> {
//                                StatusTimelineItem(
//                                    title = "Rejected",
//                                    date = currentClaim?.submissionDate, // In real app, this would be separate date
//                                    isCompleted = true,
//                                    isActive = true
//                                )
//                            }
//                            else -> {
//                                StatusTimelineItem(
//                                    title = "Decision Pending",
//                                    date = null,
//                                    isCompleted = false,
//                                    isActive = false
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Response Message (if any)
//            currentClaim?.?.let { message ->
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = when (currentClaim?.status) {
//                            ClaimStatus.APPROVED, ClaimStatus.PAID -> SabiBankColors.SuccessLight
//                            ClaimStatus.REJECTED -> SabiBankColors.ErrorLight
//                            else -> MaterialTheme.colorScheme.surfaceVariant
//                        }
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Text(
//                            text = "Response from Claims Team",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                            color = when (currentClaim?.status) {
//                                ClaimStatus.APPROVED, ClaimStatus.PAID -> SabiBankColors.Success
//                                ClaimStatus.REJECTED -> SabiBankColors.Error
//                                else -> MaterialTheme.colorScheme.onSurfaceVariant
//                            }
//                        )
//                        Text(
//                            text = message,
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = when (currentClaim?.status) {
//                                ClaimStatus.APPROVED, ClaimStatus.PAID -> SabiBankColors.Success
//                                ClaimStatus.REJECTED -> SabiBankColors.Error
//                                else -> MaterialTheme.colorScheme.onSurfaceVariant
//                            }
//                        )
//                    }
//                }
//            }

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

@Composable
fun StatusTimelineItem(
    title: String,
    date: Date?,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.secondary
                        else -> SabiBankColors.Gray300
                    },
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCompleted || isActive -> MaterialTheme.colorScheme.onSurface
                    else -> SabiBankColors.TextSecondary
                }
            )
            date?.let {
                Text(
                    text = DateUtils.formatDate(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = SabiBankColors.TextSecondary
                )
            } ?: run {
                if (!isCompleted) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = SabiBankColors.TextSecondary
                    )
                }
            }
        }
    }
} 