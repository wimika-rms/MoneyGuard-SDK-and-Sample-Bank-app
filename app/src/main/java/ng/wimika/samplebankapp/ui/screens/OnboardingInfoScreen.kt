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
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.R
import androidx.compose.ui.graphics.Color
import ng.wimika.moneyguard_sdk.services.in_app_content.models.InAppContentResponse
import ng.wimika.moneyguard_sdk.services.in_app_content.models.MobileContentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInfoScreen(
    onGetStarted: () -> Unit,
    onLearnMore: (String) -> Unit,
    onBack: () -> Unit
) {
    val sdkService = MoneyGuardClientApp.sdkService
    val token = MoneyGuardClientApp.preferenceManager?.getMoneyGuardToken()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var onboardingSlides by remember { mutableStateOf<List<MobileContentItem>>(emptyList()) }
    var learnMoreUrl by remember { mutableStateOf("") }
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
        if (sdkService != null && !token.isNullOrEmpty()) {
            try {
                val result = sdkService.inAppContent()?.getInAppContent(token, 1)
                result?.onSuccess { response ->
                    onboardingSlides = response.onboardingSlides
                    // Set learn more URL - you may need to adjust this based on your requirements
                    learnMoreUrl = "https://moneyguard.ng/learn-more" // Default fallback
                }?.onFailure { exception ->
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
            TopAppBar(
                title = {
                    Text(
                        text = "MoneyGuard Info",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                }
            )
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
            if (onboardingSlides.isNotEmpty()) {
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
                                        if (onboardingSlides.size > 1) {
                                            val threshold = 50f
                                            when {
                                                dragAmount.x > threshold && currentPage > 0 -> {
                                                    currentPage--
                                                }
                                                dragAmount.x < -threshold && currentPage < onboardingSlides.size - 1 -> {
                                                    currentPage++
                                                }
                                            }
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currentSlide = onboardingSlides[currentPage]
                            // Title (bold, large)
                            currentSlide.title?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            // Body/Description (regular)
                            currentSlide.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = Color(0xFF6B6B6B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                    // Pager indicator
                    if (onboardingSlides.size > 1) {
                        Row(
                            modifier = Modifier.height(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(onboardingSlides.size) { index ->
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
                    if (learnMoreUrl.isNotEmpty()) {
                        TextButton(
                            onClick = { onLearnMore(learnMoreUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Learn More",
                                color = Color(0xFF8854F6),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
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