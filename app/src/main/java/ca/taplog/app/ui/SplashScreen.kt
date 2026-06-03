package ca.taplog.app.ui.ember

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Brand colors hardcoded — splash renders before MaterialTheme is available
private val NavyBg    = Color(0xFF0B1F3A)
private val TealMid   = Color(0xFF1D9E75)
private val TealDeep  = Color(0xFF0F6E56)
private val TealLight = Color(0xFF5DCAA5)
private val White     = Color(0xFFFFFFFF)
private val GrayMuted = Color(0xFF888780)

@Composable
fun SplashScreen(
    registryReady: Boolean,
    onSplashComplete: () -> Unit = {},
    onNavigateToRoleSelection: (() -> Unit)? = null,
    onNavigateToFieldAnalystDashboard: (() -> Unit)? = null
) {
    val iconAlpha    = remember { Animatable(0f) }
    val iconScale    = remember { Animatable(0.85f) }
    val textAlpha    = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val readyAlpha   = remember { Animatable(0f) }

    // Entrance animations
    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(500, easing = EaseOutCubic)) }
        launch { iconScale.animateTo(1f, tween(500, easing = EaseOutCubic)) }
        delay(200)
        textAlpha.animateTo(1f, tween(400, easing = EaseOutCubic))
        delay(150)
        taglineAlpha.animateTo(1f, tween(350, easing = EaseOutCubic))
    }

    // Show tap-to-continue hint once registry is ready
    LaunchedEffect(registryReady) {
        if (registryReady) {
            readyAlpha.animateTo(1f, tween(400))
        }
    }

    // Full-screen tap target — only fires when registry is ready
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (registryReady) onSplashComplete()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Icon
            Canvas(
                modifier = Modifier
                    .size(96.dp)
                    .alpha(iconAlpha.value)
                    .scale(iconScale.value)
            ) {
                drawTapLogIcon(this)
            }

            Spacer(Modifier.height(28.dp))

            // Wordmark — Tap bold white + Log regular teal
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = White)) {
                        append("Tap")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = TealMid)) {
                        append("Log")
                    }
                },
                fontSize = 40.sp,
                letterSpacing = (-1.5).sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(10.dp))

            // Tagline
            Text(
                text = "TAP. LOG. DONE.",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = GrayMuted,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(56.dp))

            // Tap to continue hint — fades in when registryReady
            Text(
                text = "tap to continue",
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = TealLight.copy(alpha = 0.7f),
                modifier = Modifier.alpha(readyAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            // Ready dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(readyAlpha.value)
                    .background(TealLight, shape = CircleShape)
            )
        }
    }
}

// ── Icon drawing ───────────────────────────────────────────────────────────
private fun drawTapLogIcon(scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    val sx = w / 96f
    val sy = h / 96f

    val bgPath = Path().apply {
        addRoundRect(RoundRect(
            rect = Rect(Offset.Zero, Size(w, h)),
            cornerRadius = CornerRadius(18f * sx, 18f * sy)
        ))
    }
    scope.drawPath(bgPath, color = NavyBg)

    scope.clipPath(bgPath) {

        // NFC arcs
        drawNfcArc(this, 48f * sx, 62f * sy, 16f * sx, 14f * sy, 3.5f * sx, TealMid, 1f)
        drawNfcArc(this, 48f * sx, 62f * sy, 26f * sx, 24f * sy, 2.5f * sx, TealMid, 0.45f)
        drawNfcArc(this, 48f * sx, 62f * sy, 36f * sx, 34f * sy, 1.5f * sx, TealMid, 0.2f)

        // Phone body
        drawRoundRect(
            color        = TealMid,
            topLeft      = Offset(36f * sx, 52f * sy),
            size         = Size(24f * sx, 32f * sy),
            cornerRadius = CornerRadius(5f * sx, 5f * sy)
        )

        // Screen
        drawRoundRect(
            color        = TealDeep,
            topLeft      = Offset(39f * sx, 55f * sy),
            size         = Size(18f * sx, 14f * sy),
            cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy)
        )

        // Log lines
        val lx = 41f * sx
        val ly = 58f * sy
        val lg = 3.5f * sy
        val lh = 1.5f * sy
        val lr = CornerRadius(0.75f * sx, 0.75f * sy)
        drawRoundRect(color = TealLight,                     topLeft = Offset(lx, ly),        size = Size(10f * sx, lh), cornerRadius = lr)
        drawRoundRect(color = TealLight.copy(alpha = 0.6f),  topLeft = Offset(lx, ly + lg),   size = Size(7f  * sx, lh), cornerRadius = lr)
        drawRoundRect(color = TealLight.copy(alpha = 0.35f), topLeft = Offset(lx, ly + lg*2), size = Size(9f  * sx, lh), cornerRadius = lr)

        // Home bar
        drawRoundRect(
            color        = TealDeep,
            topLeft      = Offset(44f * sx, 78f * sy),
            size         = Size(8f * sx, 2f * sy),
            cornerRadius = CornerRadius(1f * sx, 1f * sy)
        )

        // Tap dot
        drawCircle(color = TealLight, radius = 4f * sx, center = Offset(48f * sx, 38f * sy))
    }
}

private fun drawNfcArc(
    scope: DrawScope,
    cx: Float, baseY: Float,
    radiusX: Float, radiusY: Float,
    strokeW: Float, color: Color, alpha: Float
) {
    val path = Path().apply {
        moveTo(cx - radiusX, baseY)
        cubicTo(
            cx - radiusX, baseY - radiusY * 1.3f,
            cx + radiusX, baseY - radiusY * 1.3f,
            cx + radiusX, baseY
        )
    }
    scope.drawPath(path, color = color.copy(alpha = alpha), style = Stroke(width = strokeW, cap = StrokeCap.Round))
}
