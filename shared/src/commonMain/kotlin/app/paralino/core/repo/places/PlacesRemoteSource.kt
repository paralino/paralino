package app.paralino.core.repo.places

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class PlacesRemoteSource(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val mapper: PlaceMapperCore,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository
) {

    suspend fun createPlace(
        groupId: String,
        private: Boolean,
        data: GroupPlaceData,
        myKey: AsymmetricCryptoKeyData
    ): GroupPlacesWithSettingsRaw {
        return blocking {
            val groupKey = cryptoKEKRepo.getLatestGroupKey(groupId)!!

            val encryptedData = cryptoCore.encryptModel(
                data = data,
                key = groupKey.data.content
            )

            val payload = CreatePlacePayload(
                groupId = groupId,
                ownerPublicKey = myKey.publicKey,
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
                    null,
                    "Unable to create place",
                    Exception(response.stderr)
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
                api.getDocumentsFor(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                    "placeId" to placeId
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
            val places = api.getDocumentsFor(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPlacesID,
                "groupId" to groupId
            ).map { it.parsePlace() }

            val settings = api.getDocumentsFor(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                "groupId" to groupId
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
            val places = api.getDocuments(
                databaseType = BackendDatabaseType.DATA,
                remoteConfig.resourceIds.collectionPlacesID
            ).map { it.parsePlace() }
            val settings = api.getDocumentsFor(
                databaseType = BackendDatabaseType.DATA,
                remoteConfig.resourceIds.collectionPlacesSettingsID
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
        data: GroupPlace
    ): GroupPlaceRaw {
        return blocking {
            val mapped = mapper.core2Remote(data)
            api.createOrUpdateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionPlacesID,
                docId = mapped.id,
                data = mapped.toPostJson()
            ).parsePlace()
        }
    }

    suspend fun updatePlaceSettings(
        data: GroupPlaceSettings
    ): GroupPlaceRaw {
        return blocking {
            val mapped = mapper.settingsMapper.core2Remote(data)
            api.createOrUpdateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionPlacesSettingsID,
                docId = mapped.id,
                data = mapped.toPostJson()
            ).parsePlace()
        }
    }

    suspend fun deletePlace(placeId: String) {
        blocking {
            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionPlaceDelete,
                data = placeId
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to delete place",
                    Exception(data.stderr)
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

