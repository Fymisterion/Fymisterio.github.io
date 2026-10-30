package de.gerry.dranbleiben

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.gerry.dranbleiben.ui.screens.CalendarScreen
import de.gerry.dranbleiben.ui.screens.CheckInScreen
import de.gerry.dranbleiben.ui.screens.HabitEditScreen
import de.gerry.dranbleiben.ui.screens.HabitsScreen
import de.gerry.dranbleiben.ui.screens.MoreScreen
import de.gerry.dranbleiben.ui.screens.TodayScreen
import de.gerry.dranbleiben.ui.theme.DranbleibenTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val app get() = application as DranbleibenApp

    /** Aus der Erinnerung geöffnet → gleich die Abfrage zeigen */
    private var checkInRequest by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(EXTRA_CHECK_IN, false) == true && savedInstanceState == null) checkInRequest++
        askForNotifications()
        setContent {
            DranbleibenTheme {
                val nav = rememberNavController()
                val current by nav.currentBackStackEntryAsState()
                val route = current?.destination?.route
                var day by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
                val date = LocalDate.parse(day)

                LaunchedEffect(checkInRequest) {
                    if (checkInRequest > 0 && route != "abfrage") nav.navigate("abfrage") { launchSingleTop = true }
                }

                Scaffold(
                    bottomBar = {
                        if (route in TABS.map { it.route }) {
                            NavigationBar {
                                TABS.forEach { tab ->
                                    NavigationBarItem(
                                        selected = route == tab.route,
                                        onClick = { nav.openTab(tab.route) },
                                        icon = { Icon(tab.icon, null) },
                                        label = { Text(tab.label) },
                                    )
                                }
                            }
                        }
                    },
                ) { outer ->
                    val padding = PaddingValues(bottom = outer.calculateBottomPadding())
                    NavHost(nav, startDestination = "heute") {
                        composable("heute") {
                            TodayScreen(
                                store = app.store,
                                date = date,
                                onDateChange = { day = it.toString() },
                                onCheckIn = { nav.navigate("abfrage") },
                                onNewHabit = { nav.navigate("bearbeiten/neu") },
                                contentPadding = padding,
                            )
                        }
                        composable("kalender") { CalendarScreen(app.store, padding) }
                        composable("gewohnheiten") {
                            HabitsScreen(app.store, padding, onOpen = { nav.navigate("bearbeiten/$it") }, onNew = { nav.navigate("bearbeiten/neu") })
                        }
                        composable("mehr") { MoreScreen(app, padding) }
                        composable("bearbeiten/{uid}") { entry ->
                            val uid = entry.arguments?.getString("uid")?.takeIf { it != "neu" }
                            HabitEditScreen(app.store, uid, onBack = { nav.popBackStack() })
                        }
                        composable("abfrage") {
                            CheckInScreen(app.store, onClose = {
                                day = LocalDate.now().toString()
                                if (!nav.popBackStack()) nav.navigate("heute")
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_CHECK_IN, false)) checkInRequest++
    }

    override fun onStart() {
        super.onStart()
        app.sync.requestSync()
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("askedNotifications", false)) return
        prefs.edit().putBoolean("askedNotifications", true).apply()
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private data class Tab(val route: String, val label: String, val icon: ImageVector)

    companion object {
        const val EXTRA_CHECK_IN = "checkIn"

        private val TABS = listOf(
            Tab("heute", "Heute", Icons.Outlined.Today),
            Tab("kalender", "Kalender", Icons.Outlined.CalendarMonth),
            Tab("gewohnheiten", "Gewohnheiten", Icons.Outlined.Checklist),
            Tab("mehr", "Mehr", Icons.Outlined.MoreHoriz),
        )
    }
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
