package cz.vanama.zbox

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vanama.blesdk.BleManager
import cz.vanama.blesdk.model.BleScanState
import cz.vanama.zbox.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BleViewModel(private val bleManager: BleManager) : ViewModel() {

    private var scanning: Boolean = false
    private var _devices = mutableListOf<BleDevice>()

    private var _uiState = MutableStateFlow<ScanState>(ScanState.Success(_devices, false))
    val uiState = _uiState.asStateFlow()

    private fun startScan() {
        _devices.clear()
        bleManager.startScan { status ->
            _uiState.value = when (status) {
                is BleScanState.Scanning -> {
                    scanning = true
                    ScanState.Success(_devices.toList(), scanning)
                }

                is BleScanState.Idle -> {
                    scanning = false
                    ScanState.Success(_devices.toList(), scanning)
                }

                is BleScanState.FoundDevice -> {
                    if (!_devices.map { it.device }.contains(status.device)) {
                        _devices.add(BleDevice(status.device))
                    }
                    ScanState.Success(_devices.toList(), scanning)
                }

                is BleScanState.Error -> ScanState.Error(status.error.message)
            }
        }
    }

    private fun stopScan() {
        bleManager.stopScan()
        scanning = false
        _uiState.value = ScanState.Success(_devices.toList(), scanning)
    }

    private fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            changeConnectionStatus(device, BleDevice.Status.CONNECTING)

            bleManager.connect(device) { gatt, error ->
                gatt?.let {
                    changeConnectionStatus(gatt.device, BleDevice.Status.CONNECTED)
                }
                error?.let {
                    changeConnectionStatus(device, BleDevice.Status.ERROR)
                }
            }
        }
    }

    private fun disconnect(device: BluetoothDevice) {
        bleManager.disconnect()
        changeConnectionStatus(device, BleDevice.Status.DISCONNECTED)
    }

    private fun changeConnectionStatus(device: BluetoothDevice, status: BleDevice.Status) {
        _devices = _devices.map {
            if (it.device.address == device.address) {
                it.copy(status = status)
            } else {
                it
            }
        }.toMutableList()
        _uiState.value = ScanState.Success(_devices.toList(), scanning)
    }

    fun onEvent(event: Event) = when (event) {
        is Event.Connect -> connect(event.device)
        is Event.Disconnect -> disconnect(event.device)
        is Event.StartScan -> startScan()
        is Event.StopScan -> stopScan()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
    }

    sealed class Event {
        data class Connect(val device: BluetoothDevice) : Event()
        data class Disconnect(val device: BluetoothDevice) : Event()
        object StartScan : Event()
        object StopScan : Event()
    }

    sealed class ScanState {
        data class Success(
            val devices: List<BleDevice>,
            val isScanning: Boolean
        ) : ScanState()

        data class Error(val errorMessage: String) : ScanState()
    }
}