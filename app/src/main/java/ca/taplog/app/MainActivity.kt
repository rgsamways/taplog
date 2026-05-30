package ca.taplog.app

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.taplog.app.ui.theme.TapLogTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var tagId by mutableStateOf<String?>(null)
    private var nfcAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAvailable = nfcAdapter != null

        setContent {
            TapLogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        nfcAvailable = nfcAvailable,
                        tagId = tagId
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        tag?.let {
            val id = it.id.joinToString("") { byte -> "%02X".format(byte) }
            tagId = id
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    nfcAvailable: Boolean = false,
    tagId: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TapLog",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap. Log. Done.",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (!nfcAvailable) {
            Text(
                text = "NFC not available on this device",
                color = MaterialTheme.colorScheme.error
            )
        } else if (tagId == null) {
            Text(
                text = "Tap an NFC tag to begin",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Tag detected",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tagId,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}