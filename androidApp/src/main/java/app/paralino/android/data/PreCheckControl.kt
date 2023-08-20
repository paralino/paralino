package app.paralino.android.data

import android.os.StrictMode
import app.paralino.core.repo.base.BaseRepository

class PreCheckControl() : BaseRepository() {

    fun start() {
        if (app.paralino.android.BuildConfig.internalTesting) {
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build()
        }
    }
}