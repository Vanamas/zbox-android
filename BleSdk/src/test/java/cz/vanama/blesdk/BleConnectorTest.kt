package cz.vanama.blesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import cz.vanama.blesdk.model.BleErrorCode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class BleConnectorTest : StringSpec({

    val context = mockk<Context>()
    val blePermissionChecker = mockk<BlePermissionChecker>()
    val bluetoothDevice = mockk<BluetoothDevice>()
    val bluetoothGatt = mockk<BluetoothGatt>()

    val bleConnector = BleConnector(context, blePermissionChecker)

    beforeTest {
        clearAllMocks()
    }

    "when connect, and has permission, should call device connectGatt" {
        every { blePermissionChecker.hasBluetoothConnect } returns true
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns bluetoothGatt

        bleConnector.connect(bluetoothDevice) { _, _ -> }

        verify { bluetoothDevice.connectGatt(context, false, any<BluetoothGattCallback>()) }
    }

    "when connect, and no permission, should not call device connectGatt and return error" {
        every { blePermissionChecker.hasBluetoothConnect } returns false
        var returnedGatt: BluetoothGatt? = null
        var returnedError: BleErrorCode? = null

        bleConnector.connect(bluetoothDevice) { gatt, error ->
            returnedGatt = gatt
            returnedError = error
        }

        verify(exactly = 0) { bluetoothDevice.connectGatt(any(), any(), any()) }
        returnedGatt shouldBe null
        returnedError shouldBe BleErrorCode.BLUETOOTH_CONNECT_PERMISSION_MISSING
    }
})
