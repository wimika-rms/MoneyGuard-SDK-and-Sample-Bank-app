package ng.wimika.samplebankapp.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Sabi Bank Brand Colors
object SabiBankColors {
    val OrangePrimary = Color(0xFFD95F29)
    val OrangeDark = Color(0xFFC05425)
    val OrangeLight = Color(0xFFE87B4C)
    val OrangeLighter = Color(0xFFFAEFE9)
    
    // Neutral Colors
    val White = Color.White
    val Black = Color.Black
    val Gray50 = Color(0xFFFAFAFA)
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEEEEEE)
    val Gray300 = Color(0xFFE0E0E0)
    val Gray400 = Color(0xFFBDBDBD)
    val Gray500 = Color(0xFF9E9E9E)
    val Gray600 = Color(0xFF757575)
    val Gray700 = Color(0xFF616161)
    val Gray800 = Color(0xFF424242)
    val Gray900 = Color(0xFF212121)
    
    // Status Colors
    val Success = Color(0xFF4CAF50)
    val SuccessLight = Color(0xFFE8F5E8)
    val Warning = Color(0xFFFF9800)
    val WarningLight = Color(0xFFFFF3E0)
    val Error = Color(0xFFF44336)
    val ErrorLight = Color(0xFFFFEBEE)
    val Info = Color(0xFF2196F3)
    val InfoLight = Color(0xFFE3F2FD)
    
    // Text Colors
    val TextOnOrange = Color.White
    val TextPrimary = Gray900
    val TextSecondary = Gray600
    val TextDisabled = Gray400
}

// Material 3 Light Color Scheme (Always Used)
val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = SabiBankColors.OrangePrimary,
    onPrimary = SabiBankColors.TextOnOrange,
    primaryContainer = SabiBankColors.OrangeLighter,
    onPrimaryContainer = SabiBankColors.OrangeDark,
    
    // Secondary colors  
    secondary = SabiBankColors.OrangeDark,
    onSecondary = SabiBankColors.White,
    secondaryContainer = Color(0xFFFFE4CC),
    onSecondaryContainer = Color(0xFF2A1800),
    
    // Tertiary colors
    tertiary = Color(0xFF705D00),
    onTertiary = SabiBankColors.White,
    tertiaryContainer = Color(0xFFFFE15D),
    onTertiaryContainer = Color(0xFF221B00),
    
    // Error colors
    error = SabiBankColors.Error,
    onError = SabiBankColors.White,
    errorContainer = SabiBankColors.ErrorLight,
    onErrorContainer = Color(0xFF410002),
    
    // Background colors
    background = SabiBankColors.White,
    onBackground = SabiBankColors.TextPrimary,
    surface = SabiBankColors.White,
    onSurface = SabiBankColors.TextPrimary,
    
    // Surface variant colors
    surfaceVariant = SabiBankColors.Gray100,
    onSurfaceVariant = SabiBankColors.Gray700,
    surfaceTint = SabiBankColors.OrangePrimary,
    
    // Inverse colors
    inverseSurface = SabiBankColors.Gray800,
    inverseOnSurface = SabiBankColors.Gray100,
    inversePrimary = SabiBankColors.OrangeLight,
    
    // Outline colors
    outline = SabiBankColors.Gray400,
    outlineVariant = SabiBankColors.Gray200,
    
    // Surface container colors
    surfaceContainer = SabiBankColors.Gray50,
    surfaceContainerHigh = SabiBankColors.Gray100,
    surfaceContainerHighest = SabiBankColors.Gray200,
    surfaceContainerLow = SabiBankColors.White,
    surfaceContainerLowest = SabiBankColors.White,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)