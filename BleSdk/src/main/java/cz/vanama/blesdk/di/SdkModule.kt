package cz.vanama.blesdk.di

import android.bluetooth.BluetoothManager
import android.content.Context
import cz.vanama.blesdk.BleConnector
import cz.vanama.blesdk.BleManager
import cz.vanama.blesdk.BlePermissionChecker
import cz.vanama.blesdk.BleScanner
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Add this module to your Koin definition and you can easily use the sdk component.
 * For example you can use the BleManager in your viewModel by viewModelOf(::YourViewModel).
 *
 * @author Martin Vana
 */
val sdkModule = module {

    singleOf(::BlePermissionChecker)
    single {
        BleScanner(
            (get<Context>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter,
            get()
        )
    }
    single { BleConnector(get(), get()) }
    single { BleManager(get(), get(), get()) }
}