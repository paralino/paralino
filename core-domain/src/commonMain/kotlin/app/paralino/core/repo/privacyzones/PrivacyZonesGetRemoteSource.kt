package app.paralino.core.repo.privacyzones

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.async

class PrivacyZonesGetRemoteSource(
    private val api: IDocumentsApi,
    private val appPrefs: AppPrefs,
) {

    suspend fun getPrivacyZone(privacyZoneId: String): GroupPrivacyZoneRaw {
        return api.getDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = BackendResourceIDs.collectionPrivacyZonesID,
            documentId = privacyZoneId,
        ).parsePrivacyZone()
    }

    suspend fun getPrivacyZonesForGroup(groupId: String): List<GroupPrivacyZoneRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPrivacyZonesID,
                queries = listOf(
                    BackendQuery.Equals(key = "groupId", value = groupId),
                    BackendQuery.Equals(key = "ownerUserId", value = appPrefs.userId),
                ),
            ).map { it.parsePrivacyZone() }
        }
    }

    suspend fun getAllPrivacyZones(): List<GroupPrivacyZoneRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPrivacyZonesID,
                queries = listOf(
                    BackendQuery.Equals(key = "ownerUserId", value = appPrefs.userId),
                ),
            ).map { it.parsePrivacyZone() }
        }
    }

    private fun Map<String, Any>.parsePrivacyZone(): GroupPrivacyZoneRaw {
        return GroupPrivacyZoneRaw.from(this)
    }
}
