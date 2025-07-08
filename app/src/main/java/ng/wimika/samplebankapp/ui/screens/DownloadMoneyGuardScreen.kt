package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.R

@Composable
fun DownloadMoneyGuardScreen(
    onLearnMore: () -> Unit = {},
    onDownloadComplete: () -> Unit = {}
) {
    val sdkService = MoneyGuardClientApp.sdkService
    val coroutineScope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.moneyguard_download_img),
                    contentDescription = "Download MoneyGuard Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Download MoneyGuard",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Now that you have enrolled, download the MoneyGuard app to enjoy all of the cyber fraud protection features it has to offer.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = Color(0xFF6B6B6B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sdkService?.utility()?.launchAppInstallation()
                            onDownloadComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6))
                ) {
                    Text(
                        text = "Download",
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onLearnMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Learn More",
                        color = Color(0xFF8854F6),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                }
            }
        }
    }

} 