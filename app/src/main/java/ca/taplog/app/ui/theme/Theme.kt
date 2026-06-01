package ca.taplog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TapLogColorScheme = darkColorScheme(
    primary = EmberRed,
    onPrimary = TextOnRed,
    primaryContainer = EmberRedLight,
    onPrimaryContainer = TextOnRed,

    background = BackgroundGrey,
    onBackground = TextPrimary,

    surface = SurfaceGrey,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceVariantGrey,
    onSurfaceVariant = TextSecondary,

    outline = OutlineGrey,

    error = Color(0xFFCF6679),
    onError = TextPrimary
)

@Composable
fun TapLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TapLogColorScheme,
        typography = Typography,
        content = content
    )
}