import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import cz.vanama.blesdk.BlePermissionChecker
import cz.vanama.blesdk.BleScanner
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BleScannerTest : StringSpec({

    val blePermissionChecker = mockk<BlePermissionChecker>(relaxed = true)

    beforeTest {
        Dispatchers.setMain(Dispatchers.Unconfined)  // Set the main dispatcher to Unconfined for testing
    }

    "startScan should not start scanning when conditions are not met" {
        val bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
        every { bluetoothAdapter.isEnabled } returns false
        every { blePermissionChecker.hasLocationPermissions() } returns false
        every { blePermissionChecker.hasBluetoothScan } returns false
        val bleScanner = spyk(BleScanner(bluetoothAdapter, blePermissionChecker), recordPrivateCalls = true)
        bleScanner.startScan(forceScan = true, timeoutMillis = 10000) {}
        verify(exactly = 0) { bluetoothAdapter.bluetoothLeScanner?.startScan(any()) }
    }

    "startScan should start scanning when conditions are met" {
        val bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
        every { bluetoothAdapter.isEnabled } returns true
        every { blePermissionChecker.hasLocationPermissions() } returns true
        every { blePermissionChecker.hasBluetoothScan } returns true
        val bleScanner = spyk(BleScanner(bluetoothAdapter, blePermissionChecker), recordPrivateCalls = true)
        bleScanner.startScan(forceScan = true, timeoutMillis = 10000) {}
        verify { bluetoothAdapter.bluetoothLeScanner?.startScan(any()) }
        bleScanner.stopScan {}
    }

    "stopScan should not stop scanning when not scanning" {
        val bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
        val bleScanner = spyk(BleScanner(bluetoothAdapter, blePermissionChecker), recordPrivateCalls = true)
        every { bleScanner.scanning } returns false
        bleScanner.stopScan()
        verify(exactly = 0) { bluetoothAdapter.bluetoothLeScanner?.stopScan(any<ScanCallback>()) }
    }

    "isBluetoothEnabled should return the correct state" {
        val bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
        every { bluetoothAdapter.isEnabled } returns true
        val bleScanner = BleScanner(bluetoothAdapter, blePermissionChecker)
        bleScanner.isBluetoothEnabled() shouldBe true
        every { bluetoothAdapter.isEnabled } returns false
        bleScanner.isBluetoothEnabled() shouldBe false
    }

})
