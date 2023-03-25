package app.paralino.core.provider

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

object KoinIOS {

    fun createIOSKoinModules(
        deviceProvider: IDeviceApiProvider
    ): List<Module> {
        return listOf(
            module {
                single(createdAtStart = true) { deviceProvider }
                single { CoroutineScopeProvider() }
                single { AppDatabaseHelper(SqlDriverFactory(), get()) }
            }
        )
    }

}