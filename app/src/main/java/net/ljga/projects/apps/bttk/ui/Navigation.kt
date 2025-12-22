/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import net.ljga.projects.apps.bttk.ui.bluetooth.DeviceDetailScreen
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
                },
                onDetailsClick = { device ->
                    viewModel.showDeviceDetails(device)
                    navController.navigate("device_details")
                }
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
