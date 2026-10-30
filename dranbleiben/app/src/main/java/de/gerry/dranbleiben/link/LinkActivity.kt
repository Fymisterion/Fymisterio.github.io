package de.gerry.dranbleiben.link

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import de.gerry.dranbleiben.DranbleibenApp
import de.gerry.dranbleiben.sync.Settings
import de.gerry.dranbleiben.ui.theme.DranbleibenTheme
import kotlinx.coroutines.launch

/** dranbleiben://verbinden?server=https://…&schluessel=… – richtet die Claude-Verbindung ein (mit Rückfrage). */
class LinkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri?.host == "verbinden") showConnect(uri) else finish()
    }

    private fun showConnect(uri: Uri) {
        val server = uri.getQueryParameter("server")?.trim()?.trimEnd('/')
        val key = uri.getQueryParameter("schluessel")?.trim()
        if (server == null || !Settings.isAllowedServer(server) || key == null || key.length < 16) {
            Toast.makeText(this, "Der Verbindungslink ist unvollständig.", Toast.LENGTH_LONG).show()
            return finish()
        }
        val host = Uri.parse(server).host ?: server
        setContent {
            DranbleibenTheme {
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                AlertDialog(
                    onDismissRequest = { if (!busy) finish() },
                    title = { Text("Mit Claude verbinden?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Dranbleiben gleicht deine Gewohnheiten dann mit diesem Server ab:")
                            Text(host, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Claude kann sie dort lesen und ändern. Nur verbinden, wenn das dein eigener Server ist.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            lifecycleScope.launch {
                                error = (application as DranbleibenApp).sync.connect(server, key)
                                busy = false
                                if (error == null) {
                                    Toast.makeText(this@LinkActivity, "Mit Claude verbunden", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                        }) { Text(if (busy) "Verbinde …" else "Verbinden") }
                    },
                    dismissButton = { TextButton(onClick = { finish() }, enabled = !busy) { Text("Abbrechen") } },
                )
            }
        }
    }
}
