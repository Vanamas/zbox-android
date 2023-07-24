package cz.vanama.zbox

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import cz.vanama.zbox.model.BleDevice
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleScreen(viewModel: BleViewModel = getViewModel()) {
    val state = viewModel.uiState.collectAsState()
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH
            )
        }
    )

    if (permissionsState.allPermissionsGranted) {
        when (val uiState = state.value) {
            is BleViewModel.ScanState.Success -> BleContent(
                devices = uiState.devices,
                uiState.isScanning,
                viewModel::onEvent
            )

            is BleViewModel.ScanState.Error -> Text(text = uiState.errorMessage)
        }
    } else {
        Column {
            val textToShow = if (permissionsState.shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                "Please request all permissions"
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                "All permissions required for this feature to be available. " +
                        "Please grant the permissions"
            }
            Text(textToShow)
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BleContent(devices: List<BleDevice>, scanning: Boolean, onEvent: (BleViewModel.Event) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) { // Makes the Column take all available space
        if (devices.isNotEmpty()) {
            Text(
                text = "Ble Devices:",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontStyle = MaterialTheme.typography.headlineMedium.fontStyle
            )
        }

        LazyColumn {
            items(devices) { device ->
                DeviceItem(device = device, onEvent = onEvent)
                Divider()
            }
        }

        if (scanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.weight(1f)) // Takes up all remaining vertical space

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) { // Centers its children horizontally
            if (scanning) {
                Button(onClick = { onEvent(BleViewModel.Event.StopScan) }) {
                    Text(text = "Stop Scan")
                }
            } else {
                Button(onClick = { onEvent(BleViewModel.Event.StartScan) }) {
                    Text(text = "Start Scan")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(device: BleDevice, onEvent: (BleViewModel.Event) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Device: ${device.device.name ?: "Unknown"} - ${device.device.address}",
            Modifier.fillMaxWidth(0.6f)
        )

        when (device.status) {
            BleDevice.Status.DISCONNECTED -> {
                Button(onClick = { onEvent(BleViewModel.Event.Connect(device.device)) }) {
                    Text(text = "Connect")
                }
            }

            BleDevice.Status.CONNECTED -> {
                Button(onClick = { onEvent(BleViewModel.Event.Disconnect(device.device)) }) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.CheckCircle),
                        contentDescription = "Connected"
                    )
                    Text(text = "Disconnect")
                }
            }

            BleDevice.Status.ERROR -> {
                Button(onClick = { onEvent(BleViewModel.Event.Connect(device.device)) }) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.Refresh),
                        contentDescription = "Error"
                    )
                    Text(text = "Reconnect")
                }
            }

            BleDevice.Status.CONNECTING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}