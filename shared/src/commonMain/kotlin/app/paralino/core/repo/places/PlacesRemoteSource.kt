package app.paralino.core.repo.places

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.raw.CreatePlacePayload
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.models.raw.PlaceEventPayload
import app.paralino.core.models.raw.PlaceNotificationsUpdatePayload
import app.paralino.core.models.raw.UpdatePlacePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.api.runAsync
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.encodeToString

class PlacesRemoteSource(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userMemberId: UserMemberIdUseCase,
    private val placesGet: PlacesGetRemoteSource
) {

    suspend fun createPlace(
        groupId: String,
        private: Boolean,
        data: GroupPlaceData,
    ): GroupPlaceWithSettingsRaw {
        val groupKey =
            cryptoKEKRepo.getGroupKey(groupId, keyVersion = null) as GroupMasterKey.Valid

        val eventSeed = cryptoCore.randomId(length = 32, AlphabetType.EXTENDED)

        val mappedData = data.copy(
            eventSeed = eventSeed
        )

        val encryptedData = cryptoCore.encryptModel(
            data = mappedData,
            key = groupKey.key
        )

        val enterEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true)
        val exitEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false)

        val payload = CreatePlacePayload(
            groupId = groupId,
            keyVersion = groupKey.version,
            private = private,
            data = encryptedData,
            eventKeys = listOf(enterEvent, exitEvent)
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.PLACE_CREATE,
            data = payload
        )

        if (response.isSuccess) {
            return getPlaceWithSettings(
                placeId = response.responseDataMap!!.get("placeId") as String
            )
        } else {
            throw response.remoteException
        }
    }

    suspend fun getPlaceWithSettings(placeId: String): GroupPlaceWithSettingsRaw {
        return placesGet.getPlaceWithSettings(placeId)
    }

    suspend fun getPlacesForGroup(groupId: String): List<GroupPlaceWithSettingsRaw> {
        return placesGet.getPlacesForGroup(groupId)
    }

    suspend fun getAllPlaces(): List<GroupPlaceWithSettingsRaw> {
        return placesGet.getAllPlaces()
    }

    suspend fun updatePlace(
        place: GroupPlace
    ): GroupPlaceRaw {
        return blocking {

            // get latest group key
            val groupKey = cryptoKEKRepo.getGroupKey(
                groupId = place.groupId,
                keyVersion = null
            )

            if (groupKey !is GroupMasterKey.Valid)
                throw MissingGroupKeyException(place.groupId, null)

            val placeData = when (place.data) {
                is CryptoResource.Ready -> place.data.data
                else -> throw CryptoException("Place data is missing: ${place.id}")
            }

            val payload: UpdatePlacePayload =
                if (placeData.eventSeed.isNullOrBlank() || place.eventKeys.isEmpty()) {
                    val eventSeed = placeData.eventSeed ?: cryptoCore.randomId(
                        length = 32,
                        AlphabetType.EXTENDED
                    )

                    val mappedData = placeData.copy(
                        eventSeed = eventSeed
                    )

                    val encryptedPlaceData = cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(mappedData),
                        key = groupKey.key
                    )

                    val enterEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true)
                    val exitEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false)

                    UpdatePlacePayload(
                        placeId = place.id,
                        placeData = encryptedPlaceData,
                        groupKeyVersion = groupKey.version,
                        eventKeys = listOf(enterEvent, exitEvent)
                    )
                } else {
                    val encryptedPlaceData = cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(placeData),
                        key = groupKey.key
                    )
                    UpdatePlacePayload(
                        placeId = place.id,
                        placeData = encryptedPlaceData,
                        groupKeyVersion = groupKey.version,
                        eventKeys = null
                    )
                }

            val response = functionsApi.runAndWait(
                function = ServerFunction.PLACE_UPDATE,
                data = payload
            )

            if (response.isSuccess) {
                response.responseDataMap!!.parsePlace()
            } else {
                throw response.remoteException
            }
        }
    }

    suspend fun updatePlaceSettings(
        groupId: String,
        placeId: String,
        placeSettingsId: String?,
        alertData: GroupPlaceSettingsData
    ): GroupPlaceSettingRaw {
        return blocking {
            val payload = PlaceNotificationsUpdatePayload(
                groupId = groupId,
                placeId = placeId,
                placeSettingsId = placeSettingsId,
                alertData = BaseRemoteModule.formatter.encodeToString(alertData)
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.SETTINGS_NOTIFICATIONS_PLACES_UPDATE,
                data = payload
            )

            if (response.isSuccess) {
                response.responseDataMap!!.parsePlaceSettings()
            } else {
                throw response.remoteException
            }
        }
    }

    suspend fun deletePlace(placeId: String) {
        blocking {
            val response = functionsApi.runAndWait(
                function = ServerFunction.PLACE_DELETE,
                data = placeId
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun notifyPlaceEnter(
        place: GroupPlace,
        timeMillis: Long
    ) {
        val enterEvent = place.eventSeed?.let {
            cryptoCore.getPlaceEventKey(seed = it, isEnter = true)
        }
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = place.id,
                locationId = userMemberId.getCurrentLocationIdForGroup(place.groupId),
                enter = true,
                event = enterEvent,
                timeDifferenceMillis = nowUTC().toEpochMilliseconds() - timeMillis
            )
        )
    }

    suspend fun notifyPlaceExit(
        place: GroupPlace,
        timeMillis: Long
    ) {
        val exitEvent = place.eventSeed?.let {
            cryptoCore.getPlaceEventKey(seed = it, isEnter = false)
        }
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = place.id,
                locationId = userMemberId.getCurrentLocationIdForGroup(place.groupId),
                enter = false,
                event = exitEvent,
                timeDifferenceMillis = nowUTC().toEpochMilliseconds() - timeMillis
            )
        )
    }

    private suspend fun notifyPlaceEvent(placePayload: PlaceEventPayload) {
        blocking {
            functionsApi.runAsync(
                function = ServerFunction.EVENT_PLACES,
                data = placePayload
            )
        }
    }

    private fun Map<String, Any>.parsePlace(): GroupPlaceRaw {
        return GroupPlaceRaw.from(this)
    }

    private fun Map<String, Any>.parsePlaceSettings(): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw.from(this)
    }
}
