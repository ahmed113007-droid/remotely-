package com.example.carremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carremote.data.DeviceStore
import com.example.carremote.ui.AddDeviceScreen
import com.example.carremote.ui.CarControlScreen
import com.example.carremote.ui.CarListScreen
import com.example.carremote.ui.theme.CarRemoteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceStore = DeviceStore(applicationContext)

        setContent {
            CarRemoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(deviceStore)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(deviceStore: DeviceStore) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val devices by deviceStore.devicesFlow.collectAsState(initial = emptyList())

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            CarListScreen(
                devices = devices,
                onDeviceClick = { device ->
                    navController.navigate("control/${device.id}")
                },
                onAddClick = { navController.navigate("add") }
            )
        }

        composable("add") {
            AddDeviceScreen(
                onBack = { navController.popBackStack() },
                onSave = { newDevice ->
                    scope.launch {
                        deviceStore.addDevice(newDevice)
                        navController.popBackStack()
                    }
                }
            )
        }

        composable("control/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            val device = devices.find { it.id == deviceId }
            if (device != null) {
                CarControlScreen(
                    device = device,
                    onBack = { navController.popBackStack() },
                    onLockedStateChanged = { newLocked ->
                        scope.launch {
                            deviceStore.updateDevice(device.copy(isLocked = newLocked))
                        }
                    }
                )
            }
        }
    }
}
