package app.paralino.core.repo.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.domain.notifications.processors.LocationRequestNotificationProcessor.LocationRequestNotificationPayload
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.CoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.coroutines.launch
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

class LocationRepositoryWrapper(
    private val locationRepo: LocationUploadEngine,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val docApi: IDocumentsApi,
    private val scopeProvider: CoroutineScopeProvider,
    private val databaseHelper: MainDatabaseHelper
) {
    
    fun startNewPushSession() {
        try {
            databaseHelper.reopen(scope = scopeProvider.userSessionIOScope)
        } catch (e: Exception) {
            logg.tag("location_service").w("Unable to start new push session: ${e.message}")
        }
    }

    fun startLocationUploadEngine() {
        locationRepo.startLocationUploadEngine()

        scopeProvider.userSessionIOScope.launch {
            try {
                locationPerformanceManager.locationBoostRequested(
                    performanceType = LocationPerformanceType.VERY_HIGH,
                    groupId = null,
                    from = "iOS LSE"
                )
                locationRepo.updateDeviceStatusOnly(tryToFetchCachedLocation = false)
            } catch (e: Exception) { }
        }
    }

    suspend fun canProcessLocationRequest(): Boolean {
        return locationSharingManager.isSharingActive()
    }

    suspend fun getGroupIdFromNotificationId(
        notificationId: String?
    ): String? {

        if (notificationId.isNullOrBlank()) {
            return null
        }

        try {
            val notification = docApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPushNotificationID,
                documentId = notificationId
            )
                .also {
                    logg.tag("location_service").v("Got notification document")
                }
                .let {
                    try {
                        PushNotificationRaw.from(it)
                    } catch (e: Exception) {
                        logg.tag("location_service").e("Error parsing push notification", e)
                        null
                    }
                }

            val payload = notification?.content?.tryParseFromJson<LocationRequestNotificationPayload>()

            logg.tag("location_service").i("Got group ${payload?.groupId} from notification")

            return payload?.groupId
        } catch (e: Exception) {
            logg.tag("location_service").i("Failed to fetch notification: ${e.message}")
            return null
        }
    }

    @OptIn(NativeRuntimeApi::class)
    fun registerNewLocation(data: LocationUpdateDataFromDevice): Payload<Unit> {
        GC.collect()
        return try {
            locationRepo.registerNewLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }
}