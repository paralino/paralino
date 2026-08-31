package app.paralino.core.domain.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.MemberLocationEntityDataUpdate
import app.paralino.core.db.main.user.UserDeviceEntityLocationDataUpdate
import app.paralino.core.domain.location.history.ILocationHistoryUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.raw.GroupLocationPatchEntry
import app.paralino.core.models.raw.LocationPatchEntry
import app.paralino.core.models.raw.LocationPatchPayload
import app.paralino.core.models.raw.LocationPatchResponse
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.UserDeviceLocationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SaveLocationUseCase(
    private val database: MainDatabaseHelper,
    private val documentApi: IDocumentsApi,
    private val appConfig: AppConfigRepository,
    private val payloadFactory: LocationUploadPayloadFactory,
    private val locationHistoryUpload: ILocationHistoryUploadEngine,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: RemoteConfigRepository
) {
    suspend fun uploadAndSaveCurrentLocations(items: List<MemberGroupIdForUpload>) {
        val threshold = remoteConfig.getCachedOrLocalConfigOnce().bulkLocationApiThreshold
        if (threshold <= 0 || items.size < threshold) {
            items.forEach { uploadAndSaveSingleEntity(it) }
        } else {
            uploadAndSaveLocationsBulk(items = items, threshold = threshold)
        }
    }

    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun uploadAndSaveCurrentLocation(data: MemberGroupIdForUpload) {
        val prepared = payloadFactory.prepare(data) ?: return
        uploadViaDocumentApi(prepared)
        triggerHistorySyncIfNeeded(listOf(prepared))
    }

    private suspend fun uploadAndSaveSingleEntity(data: MemberGroupIdForUpload) {
        try {
            uploadAndSaveCurrentLocation(data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logg.w(
                tag = "location_service",
                message = "Error saving location for group ${data.groupId}",
                t = e
            )
        }
    }

    private suspend fun uploadAndSaveLocationsBulk(
        items: List<MemberGroupIdForUpload>,
        threshold: Int
    ) {
        val prepared = items.mapNotNull { item ->
            try {
                payloadFactory.prepare(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logg.w(
                    tag = "location_service",
                    message = "Error preparing bulk location for group ${item.groupId}",
                    t = e
                )
                null
            }
        }

        try {
            if (prepared.isEmpty()) {
                return
            }

            val response = try {
                functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_PATCH,
                    data = prepared.toPatchPayload(threshold = threshold)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logg.w(
                    tag = "location_service",
                    message = "Bulk location upload failed (${e.message}), fallback to table api"
                )
                null
            }

            when {
                response == null || response.isFail || response.statusCode >= 500 -> {
                    // server did not process the patch - fallback to the document api for all items
                    prepared.forEach { tryUploadViaDocumentApi(it) }
                }

                response.isSuccess -> {
                    val result = try {
                        response.response<LocationPatchResponse>().data
                    } catch (e: Exception) {
                        logg.e(tag = "location_service", message = "Malformed bulk location response", t = e)
                        throw e
                    }

                    handleBulkResponse(result = result, prepared = prepared)
                }

                else -> {
                    throw response.remoteException
                }
            }
        } finally {
            triggerHistorySyncIfNeeded(prepared)
        }
    }

    private fun List<PreparedLocationUpload>.toPatchPayload(
        threshold: Int
    ): LocationPatchPayload {
        val personalEntries = filterIsInstance<PreparedLocationUpload.Personal>().map {
            LocationPatchEntry(
                locationId = it.locationId,
                locationData = it.locationData,
                status = it.status,
                locationHistory = it.locationHistory
            )
        }
        val groupEntries = filterIsInstance<PreparedLocationUpload.Group>().map {
            GroupLocationPatchEntry(
                locationId = it.locationId,
                locationData = it.locationData,
                status = it.status,
                locationHistory = it.locationHistory,
                deviceKeyVersion = it.deviceKeyVersion
            )
        }
        return LocationPatchPayload(
            personalLocations = personalEntries.takeIf { it.isNotEmpty() },
            groupLocations = groupEntries.takeIf { it.isNotEmpty() },
            targetBulkLocationThreshold = threshold
        )
    }

    private suspend fun handleBulkResponse(
        result: LocationPatchResponse,
        prepared: List<PreparedLocationUpload>
    ) {
        val preparedById = prepared.associateBy { it.locationId }

        result.personalResult?.updatedIds?.forEach { id ->
            val entry = preparedById[id] as? PreparedLocationUpload.Personal ?: return@forEach
            logg.v("Save new personal location to database ($id)")
            database.userDeviceDao.updateUserDeviceLocationDataPartial(
                id = id,
                locationData = entry.locationData,
                status = entry.status,
                locationHistory = entry.locationHistory
            )
        }

        result.groupResult?.updatedIds?.forEach { id ->
            val entry = preparedById[id] as? PreparedLocationUpload.Group ?: return@forEach
            logg.v("Save new group location to database ($id)")
            database.groupRelatedDao.updateMemberLocationPartial(
                id = id,
                locationData = entry.locationData,
                status = entry.status,
                locationHistory = entry.locationHistory,
                deviceKeyVersion = entry.deviceKeyVersion
            )
        }

        val errors = result.personalResult?.errors.orEmpty() + result.groupResult?.errors.orEmpty()

        errors.filter { !it.fallbackToTable }.forEach { error ->
            logg.w(
                tag = "location_service",
                message = "Bulk location upload error for ${error.locationId} (${error.key}): ${error.error?.message}"
            )
        }

        errors.filter { it.fallbackToTable }
            .mapNotNull { preparedById[it.locationId] }
            .forEach { tryUploadViaDocumentApi(it) }

        if (result.refreshConfig) {
            remoteConfig.refreshConfig(cacheTimeMillis = 20.seconds.inWholeMilliseconds)
        }
    }

    private suspend fun tryUploadViaDocumentApi(prepared: PreparedLocationUpload) {
        try {
            uploadViaDocumentApi(prepared)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logg.w(
                tag = "location_service",
                message = "Error saving location for group ${prepared.item.groupId}",
                t = e
            )
        }
    }

    private suspend fun uploadViaDocumentApi(prepared: PreparedLocationUpload) {
        val payload = buildMap<String, Any> {
            prepared.locationData?.let { put("locationData", it) }
            prepared.status?.let { put("status", it) }
            prepared.locationHistory?.let { put("locationHistory", it) }
        }

        when (prepared) {
            is PreparedLocationUpload.Personal -> {
                val newData = documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionPersonalLocationsID,
                    docId = prepared.locationId,
                    data = payload
                ).let { UserDeviceLocationRaw.from(it) }

                savePersonalLocationLocally(newData)
            }

            is PreparedLocationUpload.Group -> {
                val newData = documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appConfig.resourceIds.collectionMemberLocationsID,
                    docId = prepared.locationId,
                    data = payload.plus("deviceKeyVersion" to prepared.deviceKeyVersion)
                ).let { MemberLocationRaw.from(it) }

                saveGroupLocationLocally(newData)
            }
        }
    }

    private fun triggerHistorySyncIfNeeded(prepared: List<PreparedLocationUpload>) {
        if (prepared.any { it.shouldTriggerHistorySync }) {
            locationHistoryUpload.syncPendingEntriesInBackground(cacheTime = 2.minutes)
        }
    }

    suspend fun saveGroupLocationLocally(data: MemberLocationRaw) {
        logg.v("Save new group location to database (${data.id})")
        database.groupRelatedDao.updateMemberLocation(
            MemberLocationEntityDataUpdate(
                id = data.id,
                deviceId = data.deviceId,
                groupKeyVersion = data.groupKeyVersion,
                deviceKeyVersion = data.deviceKeyVersion,
                deviceData = data.deviceData,
                locationData = data.locationData,
                status = data.status,
                locationHistory = data.locationHistory,
            )
        )
    }

    suspend fun savePersonalLocationLocally(data: UserDeviceLocationRaw) {
        logg.v("Save new personal location to database (${data.id})")
        database.userDeviceDao.updateUserDeviceLocationData(
            UserDeviceEntityLocationDataUpdate(
                id = data.id,
                locationId = data.id,
                locationData = data.locationData,
                status = data.status,
                locationHistory = data.locationHistory,
            )
        )
    }
}
