package cz.vanama.blesdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import cz.vanama.blesdk.model.BleErrorCode
import cz.vanama.blesdk.model.BleScanState

/**
 * Manages Bluetooth Low Energy (BLE) interactions including scanning for BLE devices, connecting
 * and disconnecting from a BLE device, and setting callbacks for Bluetooth state changes. It wraps
 * the functionality provided by [BleScanner] and [BleConnector] into a unified interface.
 *
 * The main constructor expects an instance of [BleScanner] and [BleConnector] to be provided.
 * This would typically be done through dependency injection, allowing for better testing and decoupling.
 *
 * A secondary constructor is provided that automatically creates instances of [BleScanner] and
 * [BleConnector] using the given context. This is intended for use in applications that do not
 * use dependency injection (like Koin).
 *
 * @property context The application context used for creating [BleScanner] and [BleConnector]
 * in the secondary constructor.
 * @property bleScanner The [BleScanner] instance used for managing scanning of BLE devices.
 * @property bleConnector The [BleConnector] instance used for managing connections to BLE devices.
 *
 * @author Martin Vana
 */
class BleManager(
    private val context: Context,
    private val bleScanner: BleScanner,
    private val bleConnector: BleConnector
) {

    /**
     * A secondary constructor that creates [BleScanner] and [BleConnector] instances
     * using the provided context. This constructor is intended for use in applications that
     * do not use dependency injection, such as those not using Koin.
     *
     * @param context The application context.
     */
    constructor(context: Context) : this(
        context.applicationContext,
        BleScanner(context.applicationContext),
        BleConnector(context.applicationContext)
    )

    private var bluetoothStateCallback: ((Boolean) -> Unit)? = null
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent?.action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        bluetoothStateCallback?.invoke(true)
                    }

                    BluetoothAdapter.STATE_OFF -> {
                        bluetoothStateCallback?.invoke(false)
                    }
                }
            }
        }
    }
    private var isReceiverRegistered = false

    /**
     * Returns an Intent to request Bluetooth enable state.
     * @return Intent that can be used to request Bluetooth enable state.
     */
    fun getBluetoothEnableIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    /**
     * Returns an Intent to request location permissions.
     * @return Intent that can be used to request location permissions.
     */
    fun getLocationPermissionIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    /**
     * Initiates a scan for BLE devices. The provided callback function is invoked
     * with the state of scanning.
     *
     * @param callback a lambda that is called with the scan state
     */
    fun startScan(timeoutMillis: Long = 10000, callback: (state: BleScanState) -> Unit) {
        bleScanner.startScan(timeoutMillis, forceScan = false, callback)
    }

    /**
     * Stops scanning for BLE devices.
     */
    fun stopScan(callback: ((state: BleScanState) -> Unit)? = null) {
        bleScanner.stopScan(callback)
    }

    /**
     * Connects to a given BLE device. When the connection is established, the provided callback function is invoked
     * with the connected device as its argument.
     *
     * @param device the BluetoothDevice object that represents the device we want to connect to
     * @param callback a lambda that is called when a connection is established with the device
     */
    fun connect(device: BluetoothDevice, callback: (BluetoothGatt?, BleErrorCode?) -> Unit) {
        bleConnector.connect(device, callback)
    }

    /**
     * Disconnects from a connected BLE device and cleans up the resources associated with the connection.
     */
    fun disconnect() {
        bleConnector.disconnect()
    }


    /**
     * Sets the callback function for Bluetooth state changes.
     *
     * @param callback a lambda that is called when the Bluetooth state changes,
     *                 with the new state passed as the argument (true = on, false = off)
     */
    fun setBluetoothStateCallback(callback: (Boolean) -> Unit) {
        registerReceiver()
        bluetoothStateCallback = callback
    }

    /**
     * Unregisters the BroadcastReceiver used for tracking Bluetooth state changes
     * and stops BLE scan if it's in progress.
     * This method should be called when you're done using the BleManager to avoid memory leaks.
     */
    fun cleanup() {
        if (isReceiverRegistered) {
            context.unregisterReceiver(bluetoothStateReceiver)
            isReceiverRegistered = false
        }
        stopScan()
    }

    /**
     * Registers a BroadcastReceiver for Bluetooth state changes.
     */
    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bluetoothStateReceiver, filter)
            isReceiverRegistered = true
        }
    }

    companion object {
        const val TAG = "BleManager"
    }
}