package ca.taplog.app.ui.ember

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.UserRole

@Composable
fun UnregisteredTagScreen(
    tagId: String,
    userRole: UserRole,
    onRegisterAsFieldAnalyst: (tagId: String) -> Unit,
    onRegisterAsInspector: () -> Unit,
    onDismiss: () -> Unit
) {
    val teal = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // NFC ring with question mark indicator
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    for (i in 1..3) {
                        drawCircle(
                            color = teal.copy(alpha = 0.15f + i * 0.1f),
                            radius = (20f + i * 18f) * (size.width / 120.dp.toPx()),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                Text(
                    "?",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = teal
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "This asset isn't in TapLog yet",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Register it now to start its compliance record.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (userRole == UserRole.FIELD_ANALYST) {
                        onRegisterAsFieldAnalyst(tagId)
                    } else {
                        onRegisterAsInspector()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register This Asset")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now")
            }
        }
    }
}
