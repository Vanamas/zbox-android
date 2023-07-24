package cz.vanama.blesdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import cz.vanama.blesdk.model.BleErrorCode
import cz.vanama.blesdk.model.BleScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages scanning for Bluetooth Low Energy (BLE) devices. It enables the application to scan
 * for BLE devices in the surrounding area and validates necessary permissions using
 * [BlePermissionChecker] before starting a scan.
 *
 * An instance of [BleScanner] should be created by providing a [BluetoothAdapter] and a
 * [BlePermissionChecker]. The adapter is used to perform the scanning of devices, while the
 * permission checker validates that the application has the necessary permissions to carry out
 * the scanning operation.
 *
 * @property bluetoothAdapter The Bluetooth adapter used to perform scanning of BLE devices.
 * This should be obtained from the system's [BluetoothManager].
 * @property blePermissionChecker A [BlePermissionChecker] instance to validate that the
 * application has the necessary permissions for scanning BLE devices.
 *
 * @author Martin Vana
 */
class BleScanner(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val blePermissionChecker: BlePermissionChecker
) {

    /**
     * A secondary constructor that is used when the application does not use dependency injection.
     * In this case, [BluetoothAdapter] and [BlePermissionChecker] are manually instantiated using
     * the provided context.
     *
     * This constructor should only be used in apps that don't use dependency injection
     * (for example, those not using Koin).
     *
     * @param context The application context.
     */
    constructor(context: Context) : this(
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter,
        BlePermissionChecker(context)
    )

    private var scanCallback: ScanCallback? = null
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main) // for running tasks on the main thread

    @VisibleForTesting
    var scanning = false

    /**
     * Initiates a scan for BLE (Bluetooth Low Energy) devices. The provided callback function
     * is invoked with the state of scanning. If forceScan is true, any ongoing scan will be
     * stopped before starting a new one.
     *
     * @param timeoutMillis the scan duration in milliseconds. Defaults to 10,000ms (10 seconds)
     * @param forceScan a flag indicating whether the scanning process should be forced to start
     *                  even if a scan is currently ongoing
     * @param callback a lambda that is called with the scan state. The state will be one of the
     * [BleScanState] values. The callback will be invoked on the main thread.
     */
    fun startScan(
        timeoutMillis: Long = 10000,
        forceScan: Boolean = false,
        callback: (state: BleScanState) -> Unit
    ) {
        val scanStartStatus = canStartScan(forceScan)

        println(scanStartStatus.toString())
        if (!scanStartStatus.canStart) {
            scanJob?.cancel()
            callback(BleScanState.Error(scanStartStatus.errorCode ?: BleErrorCode.UNKNOWN_ERROR))
        } else {

            if (forceScan) {
                scanJob?.cancel()
            }

            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    val name = result.scanRecord?.deviceName
                    println("${BleManager.TAG}: Device name: $name / ${result.scanRecord}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        println("${BleManager.TAG}: isConnectable: ${result.isConnectable}")
                    }
                    result.device?.let { device ->
                        callback(BleScanState.FoundDevice(device))
                    }
                }
            }

            scanJob = scope.launch {
                println("${BleManager.TAG}: Scanning started")
                callback(BleScanState.Scanning)
                bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
                scanning = true
                delay(timeoutMillis)
                stopScan(callback)
            }
        }
    }

    /**
     * Stops scanning for BLE devices. Does nothing if the device is not currently scanning.
     *
     * @param callback optional callback function to be called when the scan is stopped
     */
    fun stopScan(callback: ((state: BleScanState) -> Unit)? = null) {
        if (!scanning) return // If not scanning, do nothing

        println("${BleManager.TAG}: Scanning stopped")
        scanJob?.cancel()
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanning = false
        callback?.invoke(BleScanState.Idle)
    }

    /**
     * Checks whether it is possible to start a scan for BLE devices.
     *
     * @return a [ScanStartStatus] object indicating whether the scan can start
     * and an error code if it cannot
     */
    private fun canStartScan(forceScan: Boolean): ScanStartStatus {
        val conditions = listOf(
            (!scanning || !forceScan) to BleErrorCode.ALREADY_SCANNING,
            isBluetoothEnabled() to BleErrorCode.BLUETOOTH_DISABLED,
            blePermissionChecker.hasLocationPermissions() to BleErrorCode.LOCATION_PERMISSION_MISSING,
            blePermissionChecker.hasBluetoothScan to BleErrorCode.BLUETOOTH_SCAN_PERMISSION_MISSING
        )

        for ((condition, errorCode) in conditions) {
            if (!condition) {
                println("${BleManager.TAG}: ${errorCode.message}")
                return ScanStartStatus(false, errorCode)
            }
        }

        return ScanStartStatus(true)
    }

    /**
     * Checks if Bluetooth is enabled on the device.
     *
     * @return true if Bluetooth is enabled, false otherwise
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    /**
     * Data class representing the status of starting a scan for BLE devices.
     *
     * @property canStart a Boolean indicating whether the scan can start
     * @property errorCode an optional [BleErrorCode] indicating the reason the scan cannot start
     */
    data class ScanStartStatus(val canStart: Boolean, val errorCode: BleErrorCode? = null)
}