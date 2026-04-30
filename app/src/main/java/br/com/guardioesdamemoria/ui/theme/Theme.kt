package br.com.guardioesdamemoria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.R

val ColorSeca = Color(0xFFC29245)
val ColorEnchente = Color(0xFF2E8FA3)
val ColorTempestade = Color(0xFF657189)
val ColorGeral = Color(0xFF6D756F)

val ArchiveInk = Color(0xFF17211D)
val ArchiveSurface = Color(0xFFF7F3EA)
val ArchiveCard = Color(0xFFFFFFFF)
val MemoryTeal = Color(0xFF0E6F68)
val MemoryAmber = Color(0xFFD89B35)
val NightField = Color(0xFF071713)
val NightPanel = Color(0xFF10241F)

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66D2C3),
    onPrimary = Color(0xFF003B35),
    primaryContainer = Color(0xFF0E6F68),
    onPrimaryContainer = Color(0xFFE1FFF9),
    secondary = Color(0xFFFFCA7A),
    onSecondary = Color(0xFF3F2B00),
    secondaryContainer = Color(0xFF5B4216),
    onSecondaryContainer = Color(0xFFFFE5B8),
    tertiary = Color(0xFFAFC6FF),
    background = NightField,
    onBackground = Color(0xFFE5EFEA),
    surface = NightPanel,
    onSurface = Color(0xFFE5EFEA),
    surfaceVariant = Color(0xFF223731),
    onSurfaceVariant = Color(0xFFC2D2CC),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = MemoryTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F4ED),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = MemoryAmber,
    onSecondary = Color(0xFF3A2800),
    secondaryContainer = Color(0xFFFFE4B8),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Color(0xFF49617E),
    background = ArchiveSurface,
    onBackground = ArchiveInk,
    surface = ArchiveCard,
    onSurface = ArchiveInk,
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF4C554F),
    outline = Color(0xFF7A827C)
)

@Composable
fun GuardioesDaMemoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = OutfitFont, fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = OutfitFont, fontSize = 45.sp),
        displaySmall = TextStyle(fontFamily = OutfitFont, fontSize = 36.sp),
        headlineLarge = TextStyle(fontFamily = OutfitFont, fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = OutfitFont, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = OutfitFont, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = OutfitFont, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = OutfitFont, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = OutfitFont, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = OutfitFont, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = OutfitFont, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = OutfitFont, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = OutfitFont, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = OutfitFont, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = OutfitFont, fontSize = 11.sp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
