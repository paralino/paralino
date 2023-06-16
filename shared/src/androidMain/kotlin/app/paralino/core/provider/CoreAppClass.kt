package app.paralino.core.provider

import android.annotation.SuppressLint
import android.app.Application

abstract class CoreAppClass: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: CoreAppClass
    }
}