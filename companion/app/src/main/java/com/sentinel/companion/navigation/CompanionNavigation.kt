package com.sentinel.companion.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sentinel.companion.R
import com.sentinel.companion.ui.screens.alerts.AlertsScreen
import com.sentinel.companion.ui.screens.connect.ConnectScreen
import com.sentinel.companion.ui.screens.dashboard.DashboardScreen
import com.sentinel.companion.ui.screens.devicesettings.DeviceSettingsScreen
import com.sentinel.companion.ui.screens.devicelist.DeviceListScreen
import com.sentinel.companion.ui.screens.settings.SettingsScreen
import com.sentinel.companion.ui.screens.setup.SetupWizardScreen
import com.sentinel.companion.ui.screens.stream.StreamViewerActivity
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.SurfaceLow
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val CONNECT         = "connect"
    const val DASHBOARD       = "dashboard"
    const val DEVICE_LIST     = "device_list"
    const val ALERTS          = "alerts"
    const val SETTINGS        = "settings"
    const val SETUP_WIZARD    = "setup_wizard"
    const val DEVICE_SETTINGS = "device_settings/{deviceId}"

    fun deviceSettings(deviceId: String) = "device_settings/$deviceId"
}

// ─── Bottom nav items ─────────────────────────────────────────────────────────
private data class NavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val badgeCount: Int = 0,
)

@Composable
fun CompanionNavHost(startDestination: String = Routes.CONNECT) {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            MainScaffold(navController = navController) {
                DashboardScreen(
                    onNavigateToCameras = { navController.navigate(Routes.DEVICE_LIST) },
                    onNavigateToAlerts  = { navController.navigate(Routes.ALERTS) },
                    onAddDevice         = { navController.navigate(Routes.SETUP_WIZARD) },
                )
            }
        }

        composable(Routes.DEVICE_LIST) {
            val context = LocalContext.current
            MainScaffold(navController = navController) {
                DeviceListScreen(
                    onAddDevice     = { navController.navigate(Routes.SETUP_WIZARD) },
                    onViewStream    = { deviceId -> StreamViewerActivity.launch(context, deviceId) },
                    onDeviceSettings = { deviceId -> navController.navigate(Routes.deviceSettings(deviceId)) },
                )
            }
        }

        composable(Routes.SETUP_WIZARD) {
            SetupWizardScreen(
                onDeviceSaved = { deviceId ->
                    navController.navigate(Routes.DEVICE_LIST) {
                        popUpTo(Routes.SETUP_WIZARD) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(
            route     = Routes.DEVICE_SETTINGS,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) { backStack ->
            val deviceId = backStack.arguments?.getString("deviceId") ?: return@composable
            DeviceSettingsScreen(
                deviceId  = deviceId,
                onBack    = { navController.popBackStack() },
                onDeleted = {
                    navController.navigate(Routes.DEVICE_LIST) {
                        popUpTo(Routes.DEVICE_LIST) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.ALERTS) {
            MainScaffold(navController = navController) {
                AlertsScreen()
            }
        }

        composable(Routes.SETTINGS) {
            MainScaffold(navController = navController) {
                SettingsScreen(
                    onDisconnect = {
                        navController.navigate(Routes.CONNECT) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// ─── Scaffold with bottom nav ─────────────────────────────────────────────────
@Composable
private fun MainScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    val navItems = listOf(
        NavItem(Routes.DASHBOARD,   "OVERVIEW", R.drawable.uview_icon),
        NavItem(Routes.DEVICE_LIST, "CAMERAS",  R.drawable.devices),
        NavItem(Routes.ALERTS,      "ALERTS",   R.drawable.alerts),
        NavItem(Routes.SETTINGS,    "CONFIG",   R.drawable.settings),
    )

    Scaffold(
        containerColor = BackgroundDeep,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceLow,
                tonalElevation = 0.dp,
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge(containerColor = ErrorRed) {
                                            Text(item.badgeCount.toString(), fontSize = 8.sp)
                                        }
                                    }
                                }
                            ) {
                                Image(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 8.sp,
                                fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                                letterSpacing = 0.5.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = OrangePrimary,
                            selectedTextColor   = OrangePrimary,
                            unselectedIconColor = TextDisabled,
                            unselectedTextColor = TextDisabled,
                            indicatorColor      = OrangePrimary.copy(alpha = 0.1f),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundDeep),
        ) {
            content()
        }
    }
}
