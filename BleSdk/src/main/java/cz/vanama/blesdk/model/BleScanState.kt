package cz.vanama.blesdk.model

import android.bluetooth.BluetoothDevice

sealed class BleScanState {
    object Scanning : BleScanState()
    data class FoundDevice(val device: BluetoothDevice) : BleScanState()
    object Idle : BleScanState()
    data class Error(val error: BleErrorCode) : BleScanState()
}