package com.zbapp.locationtrack

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.zbapp.locationtrack.di.KoinSetup
import com.zbapp.locationtrack.utils.logger.DebugLogger
import com.zbapp.locationtrack.utils.logger.LoggerKind

class AppClass: Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        lateinit var instance: AppClass
            private set
    }

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init(){
        instance = this
        context = applicationContext

        KoinSetup.initKoin(this)

        if(!BuildConfig.forceLogToSystem){
            Logger.setLogger(externalLogTree)
        }else{
            Logger.setLogger(DebugLogger(getString(R.string.app_name)))
        }
    }

    private val externalLogTree: LoggerKind by lazy { object: LoggerKind(){
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // TODO maybe log to file
        }
    }}

}