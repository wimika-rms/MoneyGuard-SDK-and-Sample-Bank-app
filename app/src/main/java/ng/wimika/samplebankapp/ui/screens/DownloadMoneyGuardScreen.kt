package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadMoneyGuardScreen(
    onBack: () -> Unit = {},
    onLearnMore: () -> Unit = {},
    onDownloadComplete: () -> Unit = {}
) {
    val sdkService = MoneyGuardClientApp.sdkService
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Download MoneyGuard",
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
        }
    ) { paddingValues ->
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
                            showLogoutDialog = true
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

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { 
                Text(
                    text = "MoneyGuard Download Complete",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "You'll need to login again to enable MoneyGuard."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Clear all preferences before logging out (similar to DashboardScreen)
                        preferenceManager?.saveLoggedOut(true)
                        preferenceManager?.clear()
                        
                        showLogoutDialog = false
                        onDownloadComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
} 