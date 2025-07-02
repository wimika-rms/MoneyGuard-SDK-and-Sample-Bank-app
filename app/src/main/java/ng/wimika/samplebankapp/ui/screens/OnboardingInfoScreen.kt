package ng.wimika.samplebankapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ng.wimika.moneyguard_sdk.services.onboarding_info.OnboardingInfo
import ng.wimika.moneyguard_sdk.services.onboarding_info.models.OnboardingInfoResult
import ng.wimika.samplebankapp.MoneyGuardClientApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInfoScreen(
    onGetStarted: () -> Unit,
    onLearnMore: (String) -> Unit,
    onBack: () -> Unit
) {
    val onboardingInfoService: OnboardingInfo? = MoneyGuardClientApp.sdkService?.onboardingInfo()
    val token = MoneyGuardClientApp.preferenceManager?.getMoneyGuardToken()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var onboardingData by remember { mutableStateOf<OnboardingInfoResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    
    // Function to open URL in browser
    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Load onboarding info when screen is first loaded
    LaunchedEffect(Unit) {
        if (onboardingInfoService != null && !token.isNullOrEmpty()) {
            try {
                val result = onboardingInfoService.getOnboardingInfo(token)
                result.onSuccess { data ->
                    onboardingData = data
                }.onFailure { exception ->
                    Toast.makeText(
                        context,
                        "Failed to load onboarding info: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error loading onboarding info: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        } else {
            Toast.makeText(
                context,
                "Please login to view onboarding info",
                Toast.LENGTH_SHORT
            ).show()
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome to MoneyGuard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            onboardingData?.let { data ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Carousel/Slider section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        if (data.infoList.size > 1) {
                                            val threshold = 50f
                                            when {
                                                dragAmount.x > threshold && currentPage > 0 -> {
                                                    currentPage--
                                                }
                                                dragAmount.x < -threshold && currentPage < data.infoList.size - 1 -> {
                                                    currentPage++
                                                }
                                            }
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Content for current page
                            if (data.infoList.isNotEmpty()) {
                                val currentInfo = data.infoList[currentPage]
                                
                                // Title
                                Text(
                                    text = currentInfo.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Body text
                                Text(
                                    text = currentInfo.body,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                    
                    // Page indicator - moved below the content
                    if (data.infoList.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(data.infoList.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .background(
                                            color = if (index == currentPage) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Get Started button
                        Button(
                            onClick = onGetStarted,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        // Learn More button
                        OutlinedButton(
                            onClick = { openUrl(data.learnMoreUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Learn More",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            } ?: run {
                // No data available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No onboarding information available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
} 