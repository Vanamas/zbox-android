package cz.vanama.blesdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat

/**
 * The `BlePermissionChecker` class is responsible for checking if the necessary Bluetooth Low Energy (BLE) and location permissions have been granted.
 *
 * @param context the application context
 * @author Martin Vana
 */
class BlePermissionChecker(private val context: Context) {

    /**
     * Checks if Bluetooth scanning permission is granted. This is required for Android versions S and higher.
     */
    val hasBluetoothScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        true
    }

    /**
     * Checks if Bluetooth connection permission is granted. This is required for Android versions S and higher.
     */
    val hasBluetoothConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        true
    }

    /**
     * Checks if the location permissions are granted. Requires both fine and coarse location permissions.
     * @return True if both fine and coarse location permissions are granted, False otherwise.
     */
    fun hasLocationPermissions(): Boolean {
        val hasFineLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            true
        }
        return hasFineLocation && hasCoarseLocation
    }

    /**
     * Helper function that checks if a specific permission has been granted.
     * @param permission the name of the permission to check
     * @return True if the permission is granted, False otherwise.
     */
    @VisibleForTesting
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}