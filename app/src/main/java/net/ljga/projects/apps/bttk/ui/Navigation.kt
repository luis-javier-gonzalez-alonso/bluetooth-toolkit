package net.ljga.projects.apps.bttk.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.ljga.projects.apps.bttk.ui.bluetooth.BluetoothScreen
import net.ljga.projects.apps.bttk.ui.bluetooth.BluetoothViewModel
import net.ljga.projects.apps.bttk.ui.bluetooth.ConnectionScreen
import net.ljga.projects.apps.bttk.ui.bluetooth.DeviceDetailScreen
import net.ljga.projects.apps.bttk.ui.bluetooth.GattServerScreen
import net.ljga.projects.apps.bttk.ui.dataframe.DataFrameScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "bluetooth") {
        composable("main") { DataFrameScreen(modifier = Modifier.padding(16.dp)) }
        composable("bluetooth") {
            val viewModel = hiltViewModel<BluetoothViewModel>()
            BluetoothScreen(
                viewModel = viewModel,
                onDeviceClick = { device ->
                    viewModel.connectToDevice(device)
                    navController.navigate("connection")
                },
                onDetailsClick = { device ->
                    viewModel.showDeviceDetails(device)
                    navController.navigate("device_details")
                },
                onGattServerClick = {
                    navController.navigate("gatt_server")
                }
            )
        }
        composable("gatt_server") {
            GattServerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("device_details") {
            val backStackEntry = remember(it) {
                navController.getBackStackEntry("bluetooth")
            }
            val viewModel = hiltViewModel<BluetoothViewModel>(backStackEntry)
            val state by viewModel.state.collectAsState()
            
            DeviceDetailScreen(
                device = state.selectedDevice,
                gattAliases = state.gattAliases,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("connection") {
            val backStackEntry = remember(it) {
                navController.getBackStackEntry("bluetooth")
            }
            val viewModel = hiltViewModel<BluetoothViewModel>(backStackEntry)
            val state by viewModel.state.collectAsState()
            
            ConnectionScreen(
                device = state.selectedDevice,
                isConnected = state.isConnected,
                logs = state.dataLogs,
                enabledNotifications = state.enabledNotifications,
                gattAliases = state.gattAliases,
                parserConfigs = state.parserConfigs,
                savedDataFrames = state.savedDataFrames,
                onBackClick = { navController.popBackStack() },
                onDisconnectClick = {
                    viewModel.disconnectFromDevice()
                    navController.popBackStack()
                },
                onReadCharacteristic = { s, c -> viewModel.readCharacteristic(s, c) },
                onReadDescriptors = { s, c -> viewModel.readDescriptors(s, c) },
                onWriteCharacteristic = { s, c, d -> viewModel.writeCharacteristic(s, c, d) },
                onToggleNotification = { s, c, e -> viewModel.toggleNotification(s, c, e) },
                onSaveAlias = { s, c, a -> viewModel.saveAlias(s, c, a) },
                onSaveDataFrame = { n, d -> viewModel.saveDataFrame(n, d) },
                onDeleteDataFrame = { id -> viewModel.deleteDataFrame(id) },
                onSaveParserConfig = { config -> viewModel.saveParserConfig(config) },
                onDeleteParserConfig = { s, c -> viewModel.deleteParserConfig(s, c) }
            )
        }
    }
}
