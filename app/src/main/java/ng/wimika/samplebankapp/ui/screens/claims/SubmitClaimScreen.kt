package ng.wimika.samplebankapp.ui.screens.claims

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ng.wimika.samplebankapp.R
import ng.wimika.samplebankapp.utils.DateUtils
import ng.wimika.samplebankapp.utils.FileUtils
import ng.wimika.samplebankapp.utils.PermissionUtils
import ng.wimika.samplebankapp.ui.theme.SabiBankColors
import java.util.Date
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.moneyguard_sdk.services.claims.models.Claim
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import android.util.Log
import kotlin.collections.mapNotNull
import ng.wimika.samplebankapp.MoneyGuardClientApp.Companion.preferenceManager
import java.io.InputStream

private class FileRequestBody(
    private val inputStream: InputStream,
    private val mimeType: String
) : RequestBody() {
    override fun contentType() = mimeType.toMediaTypeOrNull()
    override fun contentLength() = -1L
    override fun writeTo(sink: BufferedSink) {
        sink.writeAll(inputStream.source())
    }
}

private fun convertToMultipartBodyParts(context: Context, files: List<Uri>): List<MultipartBody.Part> {
    return files.mapNotNull { uri ->
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = FileUtils.getFileName(context, uri)
            val mimeType = FileUtils.getMimeType(context, uri) ?: "application/octet-stream"

            val requestBody = inputStream?.let { stream ->
                FileRequestBody(stream, mimeType)
            } ?: return@mapNotNull null

            MultipartBody.Part.createFormData("attachments", fileName, requestBody)
        } catch (e: Exception) {
            Log.e("SubmitClaimScreen", "Error creating MultipartBody.Part for URI: $uri", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitClaimScreen(
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Get SDK services
    val moneyGuardClaim = MoneyGuardClientApp.sdkService?.claim()
    val moneyGuardPolicy = MoneyGuardClientApp.sdkService?.policy()
    val preferenceManager = preferenceManager

    // Internal state management
    var state by remember {
        mutableStateOf(
            SubmitClaimState(
                accounts = emptyList(),
                incidentNames = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        )
    }

    // Load initial data
    LaunchedEffect(Unit) {
        val token = preferenceManager?.getMoneyGuardToken() ?: ""
        if (token.isBlank()) {
            state = state.copy(errorMessage = "No authentication token found")
            return@LaunchedEffect
        }

        state = state.copy(isLoading = true)

        // Load incident names
        moneyGuardClaim?.getIncidentNames(
            token,
            onSuccess = { names ->
                state = state.copy(incidentNames = names)
            },
            onFailure = { error ->
                error.printStackTrace()
                state = state.copy(errorMessage = "Failed to load incident names: ${error.message}")
            }
        )

        // Load user accounts
        moneyGuardPolicy?.getUserAccounts(token, partnerBankId = 101)?.fold(
            onSuccess = { response ->
                val bankAccountsWithActivePolicy = response.bankAccounts.filter { it.hasActivePolicy }
                state = state.copy(
                    accounts = bankAccountsWithActivePolicy,
                    isLoading = false
                )
            },
            onFailure = { exception ->
                exception.printStackTrace()
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Failed to load accounts: ${exception.message}"
                )
            }
        )
    }

    // Event handler function
    fun onEvent(event: SubmitClaimEvent) {
        when (event) {
            is SubmitClaimEvent.AccountSelected -> {
                state = state.copy(selectedAccount = event.account)
            }
            is SubmitClaimEvent.NameOfIncidentChanged -> {
                state = state.copy(nameofIncident = event.incidentName)
            }
            is SubmitClaimEvent.LossAmountChanged -> {
                state = state.copy(lossAmount = event.amount)
            }
            is SubmitClaimEvent.LossDateChanged -> {
                state = state.copy(lossDate = event.date, showDatePicker = false)
            }
            is SubmitClaimEvent.StatementChanged -> {
                state = state.copy(statement = event.statement)
            }
            is SubmitClaimEvent.OnFilesSelected -> {
                state = state.copy(selectedFiles = event.uris)
            }
            SubmitClaimEvent.ShowDatePicker -> {
                state = state.copy(showDatePicker = true)
            }
            SubmitClaimEvent.HideDatePicker -> {
                state = state.copy(showDatePicker = false)
            }
            SubmitClaimEvent.ShowPermissionRationale -> {
                state = state.copy(showPermissionRationale = true)
            }
            SubmitClaimEvent.HidePermissionRationale -> {
                state = state.copy(showPermissionRationale = false)
            }
            SubmitClaimEvent.SubmitClaim -> {
                state = state.copy(isLoading = true, errorMessage = null)
                
                val token = preferenceManager?.getMoneyGuardToken() ?: ""
                if (token.isBlank()) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "No authentication token found"
                    )
                    return
                }

                // Create claim object using the correct SDK structure
                val claim = Claim(
                    accountId = state.selectedAccount?.id ?: 0L,
                    lossDate = state.lossDate ?: Date(),
                    nameOfIncident = state.nameofIncident ?: "",
                    lossAmount = state.lossAmount,
                    statement = state.statement
                )

                // Convert URIs to multipart attachments
                val multipartAttachments = convertToMultipartBodyParts(context, state.selectedFiles)

                moneyGuardClaim?.submitClaim(
                    sessionToken = token,
                    claim = claim,
                    attachments = multipartAttachments,
                    onSuccess = { response ->
                        state = state.copy(
                            isLoading = false,
                            isSuccessful = true,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to submit claim"
                        )
                    }
                )
            }
        }
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onEvent(SubmitClaimEvent.OnFilesSelected(uris))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            filePickerLauncher.launch("*/*")
        } else {
            onEvent(SubmitClaimEvent.ShowPermissionRationale)
        }
    }

    if (state.isSuccessful) {
        Toast.makeText(context, "Claim submitted successfully", Toast.LENGTH_SHORT).show()
        // Reset success state and navigate back
        state = state.copy(isSuccessful = false)
        onBackPressed()
    }

    // Show error message if any
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            // Clear error after showing
            state = state.copy(errorMessage = null)
        }
    }

    if (state.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { onEvent(SubmitClaimEvent.HidePermissionRationale) },
            title = { Text("Permission Required") },
            text = { Text("Storage permission is required to select files for your claim.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(SubmitClaimEvent.HidePermissionRationale)
                        activity?.let {
                            PermissionUtils.requestPermissions(it, permissions, 100)
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SubmitClaimEvent.HidePermissionRationale) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.lossDate?.time ?: System.currentTimeMillis()
        )

        AlertDialog(
            onDismissRequest = { onEvent(SubmitClaimEvent.HideDatePicker) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    title = { Text("Select Loss Date") },
                    headline = { Text("Select Loss Date") },
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onEvent(SubmitClaimEvent.HideDatePicker)
                        }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                onEvent(SubmitClaimEvent.LossDateChanged(Date(millis)))
                            }
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Submit Claim",
                        color = SabiBankColors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier.testTag("submit_claim_back_button")
                    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Message Display
            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SabiBankColors.ErrorLight
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = SabiBankColors.Error
                        )
                        Text(
                            text = error,
                            color = SabiBankColors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            //Select Account
            ClaimsPolicyAccountsSelectionCard(
                accounts = state.accounts,
                selectedAccount = state.selectedAccount,
                onAccountSelected = { account ->
                    onEvent(SubmitClaimEvent.AccountSelected(account))
                }
            )

            // Incident Name
            ClaimsIncidentNameSelectionCard(
                incidentNames = state.incidentNames,
                selectedIncidentName = state.nameofIncident,
                onIncidentNameSelected = { incidentName ->
                    onEvent(SubmitClaimEvent.NameOfIncidentChanged(incidentName))
                }
            )

            // Loss Amount
            OutlinedTextField(
                value = if (state.lossAmount == 0.0) "" else state.lossAmount.toString(),
                onValueChange = {
                    val amount = it.toDoubleOrNull() ?: 0.0
                    onEvent(SubmitClaimEvent.LossAmountChanged(amount))
                },
                label = { Text("Loss Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_claim_loss_amount_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Loss Date
            OutlinedTextField(
                value = state.lossDate?.let {
                    DateUtils.formatDate(it)
                } ?: "",
                onValueChange = { },
                label = { Text("Loss Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_claim_loss_date_input"),
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = { onEvent(SubmitClaimEvent.ShowDatePicker) },
                        modifier = Modifier.testTag("submit_claim_date_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Statement
            OutlinedTextField(
                value = state.statement,
                onValueChange = { onEvent(SubmitClaimEvent.StatementChanged(it)) },
                label = { Text("Statement") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("submit_claim_statement_input"),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // File Selection
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
                        text = "Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            if (PermissionUtils.arePermissionsGranted(context, permissions)) {
                                filePickerLauncher.launch("*/*")
                            } else {
                                permissionLauncher.launch(permissions)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_claim_add_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Files",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Files")
                    }

                    if (state.selectedFiles.isNotEmpty()) {
                        Text(
                            text = "${state.selectedFiles.size} files selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.selectedFiles.forEachIndexed { index, uri ->
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(100.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Box {
                                        val isImage = FileUtils.isFileAnImage(context, uri)

                                        if (isImage) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(uri)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Selected file $index",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(
                                                        width = 1.dp,
                                                        shape = RoundedCornerShape(8.dp),
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.5f
                                                                ),
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.5f
                                                                )
                                                            ),
                                                            start = Offset.Zero,
                                                            end = Offset(20f, 0f)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        painter = when {
                                                            FileUtils.isFileAVideo(context, uri) -> painterResource(id = R.drawable.ic_video)
                                                            FileUtils.isFileAnAudio(context, uri) -> painterResource(id = R.drawable.ic_sound)
                                                            else -> painterResource(id = R.drawable.ic_file)
                                                        },
                                                        contentDescription = "File type icon",
                                                        modifier = Modifier.size(32.dp),
                                                        tint = SabiBankColors.Gray600
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = when {
                                                            FileUtils.isFileAVideo(context, uri) -> stringResource(
                                                                R.string.video
                                                            )
                                                            FileUtils.isFileAnAudio(context, uri) -> "Audio"
                                                            else -> "Document"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SabiBankColors.Gray600
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val updatedFiles =
                                                    state.selectedFiles.filterIndexed { i, _ -> i != index }
                                                onEvent(
                                                    SubmitClaimEvent.OnFilesSelected(
                                                        updatedFiles
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove file",
                                                tint = SabiBankColors.Error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    onEvent(SubmitClaimEvent.SubmitClaim)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("submit_claim_submit_button"),
                enabled = state.shouldEnableButton
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Submit Claim")
                }
            }
        }
    }
} 