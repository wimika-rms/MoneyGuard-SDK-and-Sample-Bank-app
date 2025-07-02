package ng.wimika.samplebankapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.moneyguard_sdk.services.onboarding_info.OnboardingInfo
import ng.wimika.moneyguard_sdk.services.onboarding_info.models.OnboardingInfoResult
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.R
import androidx.compose.ui.graphics.Color

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp)) // To balance the back arrow
            }
        },
        containerColor = Color.White
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
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo (centered at the top)
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_logo), // Replace with your logo asset
                        contentDescription = "MoneyGuard Logo",
                        tint = Color(0xFF8854F6),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Slider section
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
                            if (data.infoList.isNotEmpty()) {
                                val currentInfo = data.infoList[currentPage]
                                // Title (bold, large)
                                Text(
                                    text = currentInfo.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                // Body (regular)
                                Text(
                                    text = currentInfo.body,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = Color(0xFF6B6B6B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                    // Pager indicator
                    if (data.infoList.size > 1) {
                        Row(
                            modifier = Modifier.height(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(data.infoList.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (index == currentPage) 16.dp else 8.dp, 8.dp)
                                        .background(
                                            color = if (index == currentPage) Color(0xFF8854F6) else Color(0xFFE0E0E0),
                                            shape = MaterialTheme.shapes.small
                                        )
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    // Get Started button
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6))
                    ) {
                        Text(
                            text = "Get Started",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Learn More
                    TextButton(
                        //onClick = { onLearnMore(data.learnMoreUrl) },
                        onClick = { onLearnMore(data.learnMoreUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Learn More",
                            color = Color(0xFF8854F6),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } ?: run {
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