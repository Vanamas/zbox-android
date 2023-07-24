package cz.vanama.zbox.model

import android.bluetooth.BluetoothDevice

data class BleDevice(val device: BluetoothDevice, val status: Status = Status.DISCONNECTED) {
    enum class Status {
        DISCONNECTED, CONNECTED, CONNECTING, ERROR
    }
}
