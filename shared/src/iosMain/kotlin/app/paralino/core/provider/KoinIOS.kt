package app.paralino.core.provider

import org.koin.core.module.Module
import org.koin.dsl.module

object KoinIOS {

    fun createIOSKoinModules(
        deviceProvider: IDeviceApiProvider
    ): List<Module> {
        return listOf(
            module {
                single(createdAtStart = true) { deviceProvider }
            }
        )
    }

}