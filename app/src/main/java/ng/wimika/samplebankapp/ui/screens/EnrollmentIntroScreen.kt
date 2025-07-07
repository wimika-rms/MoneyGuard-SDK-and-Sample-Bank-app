package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.wimika.samplebankapp.R

@Composable
fun EnrollmentIntroScreen(onStartCapture: () -> Unit) {
    // Reusing the purple color from the app's theme for consistency
    val purpleColor = Color(0xFF8854F6)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Top section with purple background and the illustration card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f) // Adjusted weight to better position the card
                    .background(
                        color = purpleColor,
                        // This creates a smooth curved bottom edge for the purple area
                        shape = RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // In a real app, this would be an Image composable with a drawable resource.
                        // Using an Icon as a placeholder.
                        Image(
                            painter = painterResource(id = R.drawable.behavioral_capture),
                            contentDescription = "Behavioural Capture Illustration",
                            modifier = Modifier.size(300.dp)
                        )
//                        Icon(
//                            imageVector = R.drawable.behavioral_capture,
//                            contentDescription = "Behavioural Capture Illustration",
//                            modifier = Modifier.size(150.dp),
//                            tint = purpleColor.copy(alpha = 0.9f)
//                        )
                    }
                }
            }

            // Bottom section with descriptive text and the action button
            Column(
                modifier = Modifier
                    .weight(1f) // Takes the remaining space
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Behavioural Capture",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MoneyGuard captures your typing behaviour and uses that to secure your bank app while logging in",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This capture is in three stages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom
                Button(
                    onClick = onStartCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(50), // Pill shape
                    colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
                ) {
                    Text(
                        text = "Start Capture",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_4")
@Composable
fun EnrollmentIntroScreenPreview() {
    EnrollmentIntroScreen(onStartCapture = {})
}