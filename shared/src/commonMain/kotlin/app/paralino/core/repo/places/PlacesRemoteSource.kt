package app.paralino.core.repo.places

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.CreatePlacePayload
import app.paralino.core.models.raw.GenericFunctionPayload
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.models.raw.PlaceEventPayload
import app.paralino.core.models.raw.UpdatePlacePayload
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.fromJsonObjectToMapOfPrimitives
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
    ): GroupPlacesWithSettingsRaw {
        return blocking {
            val groupKey = cryptoKEKRepo.getGroupKey(groupId, keyVersion = null) as GroupMasterKey.Valid

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
                remoteConfig.resourceIds.functionPlaceCreate,
                BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (response.isSuccess) {
                getPlaceWithSettings(response.response)
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to create place",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    suspend fun getPlaceWithSettings(placeId: String): GroupPlacesWithSettingsRaw {
        return blocking {
            val place = api.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPlacesID,
                documentId = placeId
            ).parsePlace()

            val settings = try {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                    keyValues = listOf("placeId" to placeId)
                ).first().parsePlaceSettings()
            } catch (e: Exception) {
                null
            }

            GroupPlacesWithSettingsRaw(
                place = place,
                settings = settings
            )
        }
    }

    suspend fun getPlacesForGroup(groupId: String): List<GroupPlacesWithSettingsRaw> {
        return blocking {
            val places = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPlacesID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parsePlace() }

            val settings = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parsePlaceSettings() }

            places.map { place ->
                GroupPlacesWithSettingsRaw(
                    place = place,
                    settings = settings.firstOrNull { it.placeId == place.id }
                )
            }
        }
    }

    suspend fun getAllPlaces(): List<GroupPlacesWithSettingsRaw> {
        return blocking {
            val places = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionPlacesID
            ).map { it.parsePlace() }

            val settings = api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID
            ).map { it.parsePlaceSettings() }

            places.map { place ->
                GroupPlacesWithSettingsRaw(
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
            val mapped = mapper.core2Remote(place)

            val payload = UpdatePlacePayload(
                placeId = place.id,
                placeData = mapped.data
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionPlaceUpdate,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (response.isSuccess) {
                val response = response.response.fromJsonObjectToMapOfPrimitives()
                    .filterValues { it != null }
                    .mapValues { it.value!! }
                response.parsePlace()
            } else {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to update group",
                    cause = Exception(response.stderr)
                )
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
                functionId = remoteConfig.resourceIds.functionPlaceDelete,
                data = placeId
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to delete place",
                    cause = Exception(response.stderr)
                )
            }
        }
    }


    suspend fun notifyPlaceEnter(placeId: String) {
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = placeId,
                locationId = userMemberId.getCurrentLocationIdForPlace(placeId),
                enter = true
            )
        )
    }

    suspend fun notifyPlaceExit(placeId: String) {
        notifyPlaceEvent(
            PlaceEventPayload(
                placeId = placeId,
                locationId = userMemberId.getCurrentLocationIdForPlace(placeId),
                enter = false
            )
        )
    }

    private suspend fun notifyPlaceEvent(placePayload: PlaceEventPayload) {
        blocking {

            val payload = GenericFunctionPayload(
                route = "place-notify",
                payload = placePayload
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUserEvent,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to update location",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

    private fun Map<String, Any>.parsePlace(): GroupPlaceRaw {
        return GroupPlaceRaw.from(this)
    }

    private fun Map<String, Any>.parsePlaceSettings(): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw.from(this)
    }

}

