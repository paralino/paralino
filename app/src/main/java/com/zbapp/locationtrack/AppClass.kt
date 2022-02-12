package com.zbapp.locationtrack

import Logger
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.config
import com.zbapp.core.connectionStatus
import com.zbapp.core.logg
import com.zbapp.core.utils.IConnectionStatusProvider
import com.zbapp.locationtrack.data.PreCheckControl
import com.zbapp.locationtrack.data.config.Config
import com.zbapp.locationtrack.di.KoinSetup
import com.zbapp.locationtrack.utils.logger.DebugLogger
import com.zbapp.locationtrack.utils.logger.LoggerKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import org.koin.android.ext.android.inject

class AppClass : Application(), ICoroutineScopeProvider {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: AppClass
            private set
    }


    private val applicationJob = SupervisorJob()

    override val defaultScope = CoroutineScope(Dispatchers.Default + applicationJob)
    override val defaultIOScope = CoroutineScope(Dispatchers.IO)

    private val launcher: PreCheckControl by inject()
    private val connectionStatusProvider: IConnectionStatusProvider by inject()

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        instance = this
        context = applicationContext
        connectionStatus = connectionStatusProvider

        KoinSetup.initKoin(this)
        config = Config(context)
        initLogger()

        launcher.start()
            .catch { logg.d("Error with initial refresh: ${it.message}") }
            .launchIn(defaultScope)
    }

    private fun initLogger() {
        logg = Logger
        if (!BuildConfig.forceLogToSystem) {
            Logger.setLogger(externalLogTree)
        } else {
            Logger.setLogger(DebugLogger(getString(R.string.app_name)))
        }
    }

    private val externalLogTree: LoggerKind by lazy {
        object : LoggerKind() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // TODO maybe log to file
            }
        }
    }

    override fun onTerminate() {
        applicationJob.cancel()
        super.onTerminate()
    }

}