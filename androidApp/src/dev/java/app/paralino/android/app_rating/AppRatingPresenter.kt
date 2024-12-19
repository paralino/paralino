package app.paralino.android.app_rating

import android.app.Activity
import android.app.Application
import app.paralino.core.app_rating.IAppRatingPresenter
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class AppRatingPresenter(
    private val appClass: Application,
    private val scopeProvider: ICoroutineScopeProvider
) : IAppRatingPresenter {

    private val _ratingEvent = Channel<Unit?>()
    val ratingEvent = _ratingEvent.receiveAsFlow()

    fun triggerAppRatingDialog(activity: Activity?) {
        logg.v("app_rating", "App rating no-op")
    }

    override fun startAppRatingFlow() {
        _ratingEvent.trySend(Unit)
    }
}