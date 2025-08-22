package ng.wimika.samplebankapp.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.R

// --- NEW ---
// TODO: Replace with the actual package name of your MoneyGuard app
private const val MONEYGUARD_PACKAGE_NAME = "com.wimika.moneyguard"

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
    val context = LocalContext.current

    // --- NEW STATE MANAGEMENT ---
    // State to decide whether to show the download UI or the installation complete UI
    var showInstallComplete by remember { mutableStateOf(false) }
    // State to trigger the periodic check after the download button is clicked
    var isCheckingForApp by remember { mutableStateOf(false) }


    // --- NEW: PERIODIC CHECK LOGIC ---
    // This effect runs when `isCheckingForApp` becomes true.
    // It will be cancelled automatically if the user navigates away from this screen.
    LaunchedEffect(isCheckingForApp) {
        if (isCheckingForApp) {
            while (true) { // Loop indefinitely until the app is found or the effect is cancelled
                if (isAppInstalled(context, MONEYGUARD_PACKAGE_NAME)) {
                    // App found! Update the UI state and stop checking.
                    showInstallComplete = true
                    isCheckingForApp = false
                    break // Exit the loop
                }
                // Wait for 5 seconds before the next check
                delay(5_000L)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Download",
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
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // --- NEW: CONDITIONAL UI ---
            // Show different content based on whether the installation is complete
            if (showInstallComplete) {
                InstallCompleteContent(
                    onContinue = {
                        // Navigation will handle the logout process
                        onDownloadComplete() // This will navigate to the Login screen
                    }
                )
            } else {
                DownloadContent(
                    onDownloadClick = {
                        coroutineScope.launch {
                            sdkService?.utility()?.launchAppInstallation()
                            // After launching the Play Store, start the periodic check
                            isCheckingForApp = true
                        }
                    },
                    onLearnMore = onLearnMore
                )
            }
        }
    }
}

/**
 * --- NEW HELPER COMPOSABLE ---
 * The original UI content for downloading the app.
 */
@Composable
private fun DownloadContent(
    onDownloadClick: () -> Unit,
    onLearnMore: () -> Unit
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
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Now that you have enrolled, download the MoneyGuard app to enjoy all of the cyber fraud protection features it has to offer.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = Color(0xFF6B6B6B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onDownloadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6))
        ) {
            Text(
                text = "Download",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
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
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
            )
        }
    }
}

/**
 * --- NEW UI COMPOSABLE ---
 * A beautiful screen to show when MoneyGuard has been successfully installed.
 */
@Composable
private fun InstallCompleteContent(onContinue: () -> Unit) {
    val primaryColor = Color(0xFF8854F6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircleOutline,
            contentDescription = "Success",
            tint = primaryColor,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Installation Complete!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF1E1E1E)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To activate protection, please logout and login afresh.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}


/**
 * --- NEW HELPER FUNCTION ---
 * Checks if a specific app is installed on the device using its package name.
 */
private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        // If getPackageInfo doesn't throw an exception, the app is installed.
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        // The exception means the package was not found.
        false
    }
}