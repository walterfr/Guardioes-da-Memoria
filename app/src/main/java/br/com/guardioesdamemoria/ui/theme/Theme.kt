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

// Cores Temáticas Sugeridas
val ColorSeca = Color(0xFF8FA25F)     // Verde Musgo (Histórico/Seca)
val ColorEnchente = Color(0xFF4A9B8E) // Turquesa (Água/Enchente)
val ColorTempestade = Color(0xFF5F7FA2) // Azul Acinzentado
val ColorGeral = Color(0xFF707070)

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFCF6679),
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E).copy(alpha = 0.7f) // Para Glassmorphism
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF018786),
    tertiary = Color(0xFFB00020),
    surface = Color(0xFFF5F5F5),
    onSurface = Color.Black,
    surfaceVariant = Color.White.copy(alpha = 0.7f)
)

@Composable
fun GuardioesDaMemoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val typography = Typography(
        headlineSmall = TextStyle(fontFamily = OutfitFont, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = OutfitFont, fontSize = 20.sp),
        bodyMedium = TextStyle(fontFamily = OutfitFont, fontSize = 16.sp),
        labelSmall = TextStyle(fontFamily = OutfitFont, fontSize = 11.sp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
