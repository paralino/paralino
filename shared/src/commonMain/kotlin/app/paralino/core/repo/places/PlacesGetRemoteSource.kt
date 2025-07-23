package app.paralino.core.repo.places

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.async

class PlacesGetRemoteSource(
    private val api: IDocumentsApi,
    private val appBuildConfig: AppBuildConfig,
    private val appPrefs: AppPrefs
) {
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

    suspend fun getPersonalPlaces(): List<GroupPlaceWithSettingsRaw> {
        return blocking {
            val places = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesID,
                    queries = listOf(
                        BackendQuery.Equals(key = "groupId", value = appPrefs.userId),
                        BackendQuery.Equals(key = "ownerUserId", value = appPrefs.userId),
                    )
                ).map { it.parsePlace() }
            }

            val settings = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionPlacesSettingsID,
                    queries = listOf(
                        BackendQuery.Equals(key = "groupId", value = appPrefs.userId),
                        BackendQuery.Equals(key = "ownerUserId", value = appPrefs.userId),
                    )
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
                    collectionId = appBuildConfig.resourceIds.collectionPlacesID,
                    queries = listOf()
                ).map { it.parsePlace() }
            }

            val settings = async {
                api.listAllDocuments(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionPlacesSettingsID
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

    private fun Map<String, Any>.parsePlace(): GroupPlaceRaw {
        return GroupPlaceRaw.from(this)
    }

    private fun Map<String, Any>.parsePlaceSettings(): GroupPlaceSettingRaw {
        return GroupPlaceSettingRaw.from(this)
    }
}