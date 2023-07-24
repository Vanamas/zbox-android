package cz.vanama.blesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import cz.vanama.blesdk.model.BleErrorCode

/**
 * This class handles the connection and disconnection to Bluetooth Low Energy (BLE) devices.
 *
 * @param context the application context
 * @author Martin Vana
 */
class BleConnector(
    private val context: Context,
    private val blePermissionChecker: BlePermissionChecker
) {

    /**
     * A secondary constructor that is used when the application does not use dependency injection.
     * In this case, [BlePermissionChecker] is manually instantiated using the provided context.
     *
     * This constructor should only be used in apps that don't use dependency injection
     * (for example, those not using Koin).
     *
     * @param context The application context.
     */
    constructor(context: Context) : this(context, BlePermissionChecker(context))

    private var bluetoothGatt: BluetoothGatt? = null

    /**
     * Establishes a connection to a given BLE device.
     * Once the connection is established, the provided callback function is invoked with
     * the connected device as its argument.
     *
     * @param device the BluetoothDevice object that represents the device we want to connect to
     * @param callback a lambda that is called when a connection is established with the device
     */
    fun connect(device: BluetoothDevice, callback: (BluetoothGatt?, BleErrorCode?) -> Unit) {
        if (!blePermissionChecker.hasBluetoothConnect) {
            println("${BleManager.TAG}: BLUETOOTH_CONNECT permissions is not granted!")
            callback(null, BleErrorCode.BLUETOOTH_CONNECT_PERMISSION_MISSING)
            return
        }
        println("${BleManager.TAG}: Starting to connect to the $device")
        device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    println("${BleManager.TAG}: Device connected")
                    gatt?.let {
                        callback(it, null)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    println("${BleManager.TAG}: Device disconnected during connection")
                    callback(null, BleErrorCode.DISCONNECTED_DURING_CONNECTION)
                }
            }
        })
    }

    /**
     * Terminates the connection to a connected BLE device and releases
     * the resources associated with the connection.
     */
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        println("${BleManager.TAG}: Device disconnected")
    }
}