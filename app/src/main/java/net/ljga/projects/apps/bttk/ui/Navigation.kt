package net.ljga.projects.apps.bttk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.apps.bttk.ui.about.AboutScreen
import net.ljga.projects.apps.bttk.ui.device_connection.ConnectionScreen
import net.ljga.projects.apps.bttk.ui.device_connection.ConnectionViewModel
import net.ljga.projects.apps.bttk.ui.device_details.DeviceDetailScreen
import net.ljga.projects.apps.bttk.ui.device_scan.DeviceScanScreen
import net.ljga.projects.apps.bttk.ui.device_scan.DeviceScanViewModel
import net.ljga.projects.apps.bttk.ui.gatt_server.GattServerScreen
import net.ljga.projects.apps.bttk.ui.logs.LogScreen
import net.ljga.projects.apps.bttk.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "device_scan") {
        composable("device_scan") {
            val viewModel = hiltViewModel<DeviceScanViewModel>()
            val connectionViewModel = hiltViewModel<ConnectionViewModel>()
            DeviceScanScreen(
                viewModel = viewModel,
                onDeviceClick = { device ->
                    connectionViewModel.connectToDevice(device)
                    navController.navigate("device_connection")
                },
                onDetailsClick = { device ->
                    viewModel.showDeviceDetails(device)
                    navController.navigate("device_details")
                },
                onGattServerClick = {
                    navController.navigate("gatt_server")
                },
                onAboutClick = {
                    navController.navigate("about")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onLogsClick = {
                    navController.navigate("logs")
                }
            )
        }
        composable("device_details") {
            val backStackEntry = remember(it) {
                navController.getBackStackEntry("device_scan")
            }
            val viewModel = hiltViewModel<DeviceScanViewModel>(backStackEntry)
            val state by viewModel.state.collectAsState()

            DeviceDetailScreen(
                device = state.selectedDevice,
                gattAliases = state.gattAliases,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("device_connection") {
            val backStackEntry = remember(it) {
                navController.getBackStackEntry("device_scan")
            }
            val viewModel = hiltViewModel<ConnectionViewModel>(backStackEntry)
            val state by viewModel.state.collectAsState()

            ConnectionScreen(
                device = state.selectedDevice,
                isConnected = state.isConnected,
                logs = state.dataLogs,
                enabledNotifications = state.enabledNotifications,
                settings = state.settings,
                savedDataFrames = state.savedDataFrames,
                onBackClick = { navController.popBackStack() },
                onDisconnectClick = {
                    viewModel.disconnectFromDevice()
                    navController.popBackStack()
                },
                onReadCharacteristic = { s, c -> viewModel.readCharacteristic(s, c) },
                onWriteCharacteristic = { s, c, d -> viewModel.writeCharacteristic(s, c, d) },
                onToggleNotification = { s, c, e -> viewModel.toggleNotification(s, c, e) },
                onSaveDataFrame = { n, d -> viewModel.saveDataFrame(n, d) },
                onDeleteDataFrame = { id -> viewModel.deleteDataFrame(id) },
                onSaveSettings = { settings -> viewModel.saveSettings(settings) },
                onDeleteParserConfig = { s, c -> viewModel.deleteParserConfig(s, c) }
            )
        }
        composable("gatt_server") {
            GattServerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("logs") {
            LogScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
