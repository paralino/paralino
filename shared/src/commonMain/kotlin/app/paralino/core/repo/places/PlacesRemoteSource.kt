package app.paralino.core.repo.places

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.CreatePlacePayload
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.models.raw.PlaceEventPayload
import app.paralino.core.models.raw.UpdatePlacePayload
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.api.runAsync
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.async
import kotlinx.serialization.encodeToString

class PlacesRemoteSource(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: PlaceMapperCore,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userMemberId: UserMemberIdUseCase
) {

    suspend fun createPlace(
        groupId: String,
        private: Boolean,
        data: GroupPlaceData,
    ): GroupPlaceWithSettingsRaw {
        val groupKey =
            cryptoKEKRepo.getGroupKey(groupId, keyVersion = null) as GroupMasterKey.Valid

        val encryptedData = cryptoCore.encryptModel(
            data = data,
            key = groupKey.key
        )

        val payload = CreatePlacePayload(
            groupId = groupId,
            keyVersion = groupKey.version,
            private = private,
            data = encryptedData
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
        return blocking {
            val place = async {
                api.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesID,
                    documentId = placeId
                ).parsePlace()
            }

            val settings = async {
                try {
                    api.listAllDocuments(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                        queries = listOf(BackendQuery.Equals(key = "placeId", value = placeId))
                    ).firstOrNull()?.parsePlaceSettings()
                } catch (e: Exception) {
                    null
                }
            }

            GroupPlaceWithSettingsRaw(
                place = place.await(),
                settings = settings.await()
            )
        }
    }

    suspend fun getPlacesForGroup(groupId: String): List<GroupPlaceWithSettingsRaw> {
        return blocking {
            val places = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parsePlace() }
            }

            val settings = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                    queries = listOf(BackendQuery.Equals(key = "groupId", value = groupId))
                ).map { it.parsePlaceSettings() }
            }.await()

            places.await().map { place ->
                GroupPlaceWithSettingsRaw(
                    place = place,
                    settings = settings.firstOrNull { it.placeId == place.id }
                )
            }
        }
    }

    suspend fun getAllPlaces(): List<GroupPlaceWithSettingsRaw> {
        return blocking {
            val places = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = remoteConfig.resourceIds.collectionPlacesID
                ).map { it.parsePlace() }
            }

            val settings = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID
                ).map { it.parsePlaceSettings() }
            }.await()

            places.await().map { place ->
                GroupPlaceWithSettingsRaw(
                    place = place,
                    settings = settings.firstOrNull { it.placeId == place.id }
                )
            }
        }
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
                is CryptoResource.Ready -> {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(place.data.data),
                        key = groupKey.key
                    )
                }

                else -> throw CryptoException("Place data is missing: ${place.id}")
            }

            val payload = UpdatePlacePayload(
                placeId = place.id,
                placeData = placeData,
                groupKeyVersion = groupKey.version
            )

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
        data: GroupPlaceSettings
    ): GroupPlaceSettingRaw {
        return blocking {
            val mapped = mapper.settingsMapper.core2Remote(data)
            if (data.id.isBlank()) {
                api.createDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID,
                    docId = null,
                    data = mapped.toPostJson()
                ).parsePlaceSettings()
            } else {
                try {
                    // first try to update document
                    api.updateDocument(
                        databaseType = BackendDatabaseType.DATA,
                        collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID,
                        docId = mapped.id,
                        data = mapped.toPostJsonUpdate()
                    ).parsePlaceSettings()
                } catch (e: Exception) {
                    if (e.asRemoteException() is NotFoundException) {
                        // if document is not found, create it
                        api.createDocument(
                            databaseType = BackendDatabaseType.DATA,
                            collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID,
                            docId = null,
                            data = mapped.toPostJson()
                        ).parsePlaceSettings()
                    } else {
                        throw e
                    }
                }
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
        placeId: String,
        groupId: String,
        timeMillis: Long
    ) {
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = placeId,
                locationId = userMemberId.getCurrentLocationIdForGroup(groupId),
                enter = true,
                timeDifferenceMillis = nowUTC().toEpochMilliseconds() - timeMillis
            )
        )
    }

    suspend fun notifyPlaceExit(
        placeId: String,
        groupId: String,
        timeMillis: Long
    ) {
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = placeId,
                locationId = userMemberId.getCurrentLocationIdForGroup(groupId),
                enter = false,
                timeDifferenceMillis = nowUTC().toEpochMilliseconds() - timeMillis
            )
        )
    }

    private suspend fun notifyPlaceEvent(placePayload: PlaceEventPayload) {
        blocking {
            functionsApi.runAsync(
                function = ServerFunction.PLACE_NOTIFY,
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
