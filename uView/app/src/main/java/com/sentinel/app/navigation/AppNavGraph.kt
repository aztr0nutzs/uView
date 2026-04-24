package com.sentinel.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sentinel.app.features.addcamera.AddCameraScreen
import com.sentinel.app.features.cameras.CameraListScreen
import com.sentinel.app.features.cameras.detail.CameraDetailScreen
import com.sentinel.app.features.dashboard.DashboardScreen
import com.sentinel.app.features.diagnostics.DiagnosticsScreen
import com.sentinel.app.features.discovery.DiscoveryScreen
import com.sentinel.app.features.events.EventsScreen
import com.sentinel.app.features.multiview.MultiViewScreen
import com.sentinel.app.features.settings.SettingsScreen

// ─────────────────────────────────────────────────────────────────────────────
// Route Definitions
// ─────────────────────────────────────────────────────────────────────────────

object Routes {
    const val DASHBOARD      = "dashboard"
    const val CAMERAS        = "cameras"
    const val MULTI_VIEW     = "multi_view"
    const val ADD_CAMERA     = "add_camera"
    const val EVENTS         = "events"
    const val SETTINGS       = "settings"
    const val DISCOVERY      = "discovery"
    const val DIAGNOSTICS    = "diagnostics"

    // Parametrised routes
    const val CAMERA_DETAIL  = "camera_detail/{cameraId}"
    fun cameraDetail(cameraId: String) = "camera_detail/$cameraId"
}

// ─────────────────────────────────────────────────────────────────────────────
// AppNavGraph
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.DASHBOARD
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ── Dashboard ─────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateCameras      = { navController.navigate(Routes.CAMERAS) },
                onNavigateMultiView    = { navController.navigate(Routes.MULTI_VIEW) },
                onNavigateAddCamera    = { navController.navigate(Routes.ADD_CAMERA) },
                onNavigateEvents       = { navController.navigate(Routes.EVENTS) },
                onNavigateSettings     = { navController.navigate(Routes.SETTINGS) },
                onNavigateDiscovery    = { navController.navigate(Routes.DISCOVERY) },
                onNavigateCameraDetail = { id -> navController.navigate(Routes.cameraDetail(id)) }
            )
        }

        // ── Camera List ───────────────────────────────────────────────────
        composable(Routes.CAMERAS) {
            CameraListScreen(
                onNavigateBack         = { navController.popBackStack() },
                onNavigateCameraDetail = { id -> navController.navigate(Routes.cameraDetail(id)) },
                onNavigateAddCamera    = { navController.navigate(Routes.ADD_CAMERA) },
                onNavigateEdit         = { id -> navController.navigate(Routes.cameraDetail(id)) }
            )
        }

        // ── Camera Detail ─────────────────────────────────────────────────
        composable(
            route = Routes.CAMERA_DETAIL,
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType })
        ) {
            CameraDetailScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // ── Multi-View Grid ───────────────────────────────────────────────
        composable(Routes.MULTI_VIEW) {
            MultiViewScreen(
                onNavigateBack         = { navController.popBackStack() },
                onNavigateCameraDetail = { id -> navController.navigate(Routes.cameraDetail(id)) },
                onNavigateAddCamera    = { navController.navigate(Routes.ADD_CAMERA) }
            )
        }

        // ── Add Camera Wizard ─────────────────────────────────────────────
        composable(Routes.ADD_CAMERA) {
            AddCameraScreen(
                onNavigateBack  = { navController.popBackStack() },
                onSaveComplete  = {
                    navController.popBackStack()
                    navController.navigate(Routes.CAMERAS)
                }
            )
        }

        // ── Events ────────────────────────────────────────────────────────
        composable(Routes.EVENTS) {
            EventsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack        = { navController.popBackStack() },
                onNavigateDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
            )
        }

        // ── Discovery / Network Scan ──────────────────────────────────────
        composable(Routes.DISCOVERY) {
            DiscoveryScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddCamera    = { _ -> navController.navigate(Routes.ADD_CAMERA) }
            )
        }

        // ── Diagnostics ───────────────────────────────────────────────────
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
