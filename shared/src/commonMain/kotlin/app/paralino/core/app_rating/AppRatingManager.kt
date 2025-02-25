package app.paralino.core.app_rating

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.days

class AppRatingManager(
    private val appPrefs: AppPrefs,
    private val appRatingPresenter: IAppRatingPresenter,
    private val deviceApiProvider: IDeviceApiProvider
): BaseRepository() {

    fun tryShowingAppRating(source: AppRatingSource) {
        if (appPrefs.lastShowedAppRatingDialogUTC.isOlderThan(180.days)) {
            val canLaunch = when (source) {
                AppRatingSource.GROUP_DETAIL -> appPrefs.appLaunchedCountWhenLoggedIn >= 2
                AppRatingSource.MAIN_MAP -> appPrefs.appLaunchedCountWhenLoggedIn >= 3
                AppRatingSource.APP_SETTINGS -> appPrefs.appLaunchedCountWhenLoggedIn >= 2
            }

            if (canLaunch) {
                logg.v("app_rating", "Start app rating flow from $source")
                appPrefs.lastShowedAppRatingDialogUTC = nowUTC()
                appPrefs.lastShowedAppRatingDialogAppVersion = deviceApiProvider.appVersionCode
                appRatingPresenter.startAppRatingFlow()
            }
        }
    }
}

enum class AppRatingSource {
    GROUP_DETAIL, MAIN_MAP, APP_SETTINGS
}
