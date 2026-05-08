package app.paralino.core.repo.places

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.GetMemberWithLocationsUseCase
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.GroupOrSpaceId
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
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.api.runAsync
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC

class PlacesRemoteSource(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val groupLocal: GetMemberWithLocationsUseCase,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val placesGet: PlacesGetRemoteSource,
    private val appPrefs: AppPrefs
) {

    suspend fun createPlace(
        groupId: String,
        private: Boolean,
        isInPersonalSpace: Boolean,
        data: GroupPlaceData,
    ): GroupPlaceWithSettingsRaw {

        val eventSeed = cryptoCore.randomId(length = 32, AlphabetType.EXTENDED)

        val mappedData = data.copy(
            eventSeed = eventSeed
        )

        val encryptedData = if (isInPersonalSpace) {
            val userKey = userKeyRepo.activePersonalKeyWithVersion()
            val encryptedData = cryptoCore.encryptModel(
                data = mappedData,
                key = userKey.key
            )

            encryptedData to userKey.version
        } else {
            val groupKey =
                cryptoKEKRepo.getGroupKey(groupId, keyVersion = null) as GroupMasterKey.Valid

            val encryptedData = cryptoCore.encryptModel(
                data = mappedData,
                key = groupKey.key
            )
            encryptedData to groupKey.version
        }

        val enterEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true)
        val exitEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false)

        val payload = CreatePlacePayload(
            groupId = if (isInPersonalSpace) appPrefs.userId else groupId,
            keyVersion = encryptedData.second,
            private = private,
            data = encryptedData.first,
            isInPersonalSpace = isInPersonalSpace,
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

    suspend fun getAllPersonalPlaces(): List<GroupPlaceWithSettingsRaw> {
        return placesGet.getPersonalPlaces()
    }

    suspend fun getAllPlaces(): List<GroupPlaceWithSettingsRaw> {
        return placesGet.getAllPlaces()
    }

    suspend fun updatePlace(
        place: GroupPlace
    ): GroupPlaceRaw {
        return blocking {

            val placeData = when (place.data) {
                is CryptoResource.Ready -> place.data.data
                else -> throw CryptoException("Place data is missing: ${place.id}")
            }

            val key = if (place.isInPersonalSpace) {
                userKeyRepo.activePersonalKeyWithVersion()
            } else {
                val groupKey = cryptoKEKRepo.getGroupKey(
                    groupId = place.groupOrSpaceId.id,
                    keyVersion = null
                )

                if (groupKey !is GroupMasterKey.Valid)
                    throw MissingGroupKeyException(
                        groupId = place.groupOrSpaceId.id,
                        keyVersion = null
                    )

                SymmetricCryptoKeyData(key = groupKey.key, version = groupKey.version)
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
                        key = key.key
                    )

                    val enterEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true)
                    val exitEvent = cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false)

                    UpdatePlacePayload(
                        placeId = place.id,
                        placeData = encryptedPlaceData,
                        groupKeyVersion = if (place.isInPersonalSpace) null else key.version,
                        userKeyVersion = if (place.isInPersonalSpace) key.version else null,
                        eventKeys = listOf(enterEvent, exitEvent)
                    )
                } else {
                    val encryptedPlaceData = cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(placeData),
                        key = key.key
                    )
                    UpdatePlacePayload(
                        placeId = place.id,
                        placeData = encryptedPlaceData,
                        groupKeyVersion = if (place.isInPersonalSpace) null else key.version,
                        userKeyVersion = if (place.isInPersonalSpace) key.version else null,
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
        groupId: GroupOrSpaceId,
        placeId: String,
        placeSettingsId: String?,
        alertData: GroupPlaceSettingsData
    ): GroupPlaceSettingRaw {
        return blocking {
            val payload = PlaceNotificationsUpdatePayload(
                groupId = groupId.id,
                placeId = placeId,
                placeSettingsId = placeSettingsId,
                // for now, personal space allows only per device settings
                deviceId = if (groupId.isPersonalSpace) {
                    appPrefs.deviceId
                } else {
                    null
                },
                alertData = BaseRemoteModule.formatter.encodeToString(alertData)
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.ALERTS_PLACES_UPDATE,
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
                locationId = groupLocal.getCurrentLocationIdForGroup(groupId = place.groupOrSpaceId),
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
                locationId = groupLocal.getCurrentLocationIdForGroup(place.groupOrSpaceId),
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
