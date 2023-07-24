import cz.vanama.blesdk.di.sdkModule
import cz.vanama.zbox.BleViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

fun appModules() = listOf(sdkModule, viewModelModule)

private val viewModelModule = module {
    viewModelOf(::BleViewModel)
}