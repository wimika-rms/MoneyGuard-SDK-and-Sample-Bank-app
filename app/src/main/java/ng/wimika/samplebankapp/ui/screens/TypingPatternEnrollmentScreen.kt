package ng.wimika.samplebankapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingPatternScreen(
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    var userInput by remember { mutableStateOf("") }
    val textToType by remember {
        mutableStateOf("nefariously valedictory breakpoints regulations")
    }
    var showSuccessBanner by remember { mutableStateOf(false) }

    // This effect triggers when the final step is completed
    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            delay(5000L) // Wait for 1 second as requested
            onRegistrationComplete()
        }
    }

    // By wrapping the Scaffold in a Box, we can place the banner on top of everything.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Please type the words shown below") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .imePadding() // <-- THIS IS THE FIX. It makes the UI keyboard-aware.
            ) {
                // Multi-step progress indicator
                MultiStepProgressBar(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                )

                // Text to be typed by the user
                Text(
                    text = textToType,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // User input field
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Type here") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F6FA),
                        unfocusedContainerColor = Color(0xFFF5F6FA),
                        disabledContainerColor = Color(0xFFF5F6FA),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.weight(1f))

                // Submit button
                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                            userInput = ""
                        } else {
                            // Final submission
                            showSuccessBanner = true
                        }
                    },
                    enabled = userInput.isNotBlank() && !showSuccessBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8854F6),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "Submit",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Success banner that overlays on top of the Scaffold
        AnimatedVisibility(
            visible = showSuccessBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding() // Ensures banner is below the system status bar
                .padding(top = 16.dp)
        ) {
            SuccessBanner()
        }
    }
}

@Composable
private fun MultiStepProgressBar(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..totalSteps) {
            val color = if (i <= currentStep) Color(0xFF8854F6) else Color(0xFFE0E0E0)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
private fun SuccessBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8854F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Typing Pattern registered",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Your Behavioural capture was successful",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }
    }
}