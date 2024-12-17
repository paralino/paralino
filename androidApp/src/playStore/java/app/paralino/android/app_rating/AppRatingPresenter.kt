package app.paralino.android.app_rating

import android.app.Activity
import android.app.Application
import app.paralino.core.app_rating.IAppRatingPresenter
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AppRatingPresenter(
    private val appClass: Application,
    private val scopeProvider: ICoroutineScopeProvider
) : IAppRatingPresenter {

    private val _ratingEvent = Channel<Unit?>()
    val ratingEvent = _ratingEvent.receiveAsFlow()

    fun triggerAppRatingDialog(activity: Activity?) {
        activity ?: return
        scopeProvider.userSessionIOScope.launch {
            try {
                logg.v("app_rating", "Trigger Android app rating dialog")
                val manager = ReviewManagerFactory.create(appClass)
                val reviewInfo = manager.requestReview()
                manager.launchReview(activity, reviewInfo)
            } catch (e: Exception) {
                logg.e("app_rating", "Error launching app rating dialog", e)
            }
        }
    }

    override fun startAppRatingFlow() {
        _ratingEvent.trySend(Unit)
    }
}