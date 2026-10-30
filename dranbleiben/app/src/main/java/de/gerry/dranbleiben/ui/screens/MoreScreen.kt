package de.gerry.dranbleiben.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gerry.dranbleiben.BuildConfig
import de.gerry.dranbleiben.DranbleibenApp
import de.gerry.dranbleiben.sync.Settings
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(app: DranbleibenApp, contentPadding: PaddingValues) {
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("Mehr") }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Notifications(app)
            HorizontalDivider()
            ClaudeConnection(app)
            HorizontalDivider()
            About()
            Box(Modifier.height(24.dp))
        }
    }
}

/** Benachrichtigungen und exakte Wecker – ohne sie kommen keine Erinnerungen */
@Composable
private fun Notifications(app: DranbleibenApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        tick++
        scope.launch { app.reminders.rescheduleAll() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val notificationsOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exact = remember(tick) { app.reminders.canScheduleExact() }

    SectionTitle("Erinnerungen")
    StatusLine(notificationsOn, if (notificationsOn) "Benachrichtigungen erlaubt" else "Benachrichtigungen sind aus – Erinnerungen kommen nicht an.")
    if (!notificationsOn) {
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }) { Text("Erlauben") }
    }
    StatusLine(exact, if (exact) "Pünktliche Erinnerungen erlaubt" else "Erinnerungen können sich um einige Minuten verspäten.")
    if (!exact && Build.VERSION.SDK_INT >= 31) {
        OutlinedButton(onClick = {
            context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }) { Text("Pünktlich erlauben") }
    }
    Text(
        "Erinnerungszeiten stellst du je Gewohnheit ein. Zur Zeit fragt dich eine Benachrichtigung ab, was noch offen ist – " +
            "eine Ja/Nein-Gewohnheit kannst du direkt dort beantworten.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text("  $text", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ClaudeConnection(app: DranbleibenApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = app.settings
    val status by app.sync.status.collectAsStateWithLifecycle()
    var connected by remember { mutableStateOf(settings.isConnected) }
    var server by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }
    var confirmDisconnect by remember { mutableStateOf(false) }

    SectionTitle("Claude-Verbindung")
    Text(
        "Claude kann deine Gewohnheiten lesen, Einträge machen („heute 20 Minuten gelesen“), neue anlegen und dir " +
            "Auswertungen schreiben – in der Claude-App, am PC und im Browser. Es ist derselbe Server und Connector wie bei Kopffrei und Teller.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (!connected) {
        Text(
            "Am einfachsten: die Einrichtungsseite deines Servers auf dem Handy öffnen und „In Dranbleiben verbinden“ antippen. " +
                "Oder hier eintragen:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = server, onValueChange = { server = it.trim() }, singleLine = true,
            label = { Text("Server-Adresse") }, placeholder = { Text("https://kopffrei-cloud….workers.dev") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = key, onValueChange = { key = it.trim() }, singleLine = true, label = { Text("Schlüssel") }, modifier = Modifier.fillMaxWidth())
        connectError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                busy = true
                connectError = null
                scope.launch {
                    connectError = app.sync.connect(server, key)
                    connected = connectError == null
                    busy = false
                }
            },
            enabled = !busy && Settings.isAllowedServer(server) && key.length >= 16,
        ) { Text(if (busy) "Verbinde …" else "Verbinden") }
        return
    }

    when {
        status.running -> Text("Gleicht ab …", style = MaterialTheme.typography.bodyMedium)
        status.error != null -> Text(status.error!!, color = MaterialTheme.colorScheme.error)
        status.lastSync > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Text("  Verbunden · abgeglichen " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(status.lastSync)))
        }
        else -> Text("Verbunden")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { app.sync.requestSync() }, enabled = !status.running) {
            Icon(Icons.Outlined.Sync, null)
            Text("  Jetzt abgleichen")
        }
        TextButton(onClick = { confirmDisconnect = true }) { Text("Trennen") }
    }
    val connector = settings.connectorUrl.orEmpty()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Adresse für Claude", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(Modifier.weight(1f)) {
                    Text(connector, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Adresse für Claude", connector))
                    Toast.makeText(context, "Kopiert", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Outlined.ContentCopy, "Kopieren") }
            }
            Text(
                "Hast du Kopffrei oder Teller schon mit Claude verbunden, ist nichts weiter zu tun – der Connector kennt Dranbleiben automatisch. " +
                    "Sonst einmalig in Claude eintragen: Einstellungen → Connectors → Eigenen Connector hinzufügen. Die Adresse ist geheim.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDisconnect) {
        ConfirmDialog(
            title = "Claude-Verbindung trennen?",
            text = "Dranbleiben gleicht nicht mehr ab. Deine Daten auf dem Handy bleiben erhalten, die Kopie auf dem Server auch.",
            confirmLabel = "Trennen",
            onDismiss = { confirmDisconnect = false },
            onConfirm = {
                app.sync.disconnect()
                connected = false
                confirmDisconnect = false
            },
        )
    }
}

@Composable
private fun About() {
    SectionTitle("Über Dranbleiben")
    Text(
        "Dranbleiben ${BuildConfig.VERSION_NAME} – Gewohnheiten mit Kalender, Serien und Erinnerungen, ohne Konto und ohne Werbung. " +
            "Alles bleibt auf deinem Handy. Ins Internet geht Dranbleiben nur, wenn du die Claude-Verbindung einrichtest.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
