package cz.vanama.blesdk.model

enum class BleErrorCode(val message: String) {
    ALREADY_SCANNING("Scanning is already started"),
    BLUETOOTH_DISABLED("Bluetooth is not enabled"),
    LOCATION_PERMISSION_MISSING("Location permissions is not granted"),
    BLUETOOTH_SCAN_PERMISSION_MISSING("BLUETOOTH_SCAN permissions is not granted"),
    BLUETOOTH_CONNECT_PERMISSION_MISSING("BLUETOOTH_CONNECT permission is missing"),
    DISCONNECTED_DURING_CONNECTION("Disconnected during connection"),
    UNKNOWN_ERROR("Unknown error")
}