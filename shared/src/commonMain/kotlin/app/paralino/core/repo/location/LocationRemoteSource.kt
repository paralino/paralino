package app.paralino.core.repo.location

import app.paralino.core.appPrefs
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.LocationDeletePayload
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException

class LocationRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository
) {

    suspend fun deleteLocation(memberId: String, locationId: String) {
        blocking {

            val payload = LocationDeletePayload(
                memberId = memberId,
                locationId = locationId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.LOCATION_DELETE,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun clearLocation(id: String) {
        blocking {
            documentApi.updateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                docId = id,
                data = mapOf(
                    "locationData" to "",
                )
            )
        }
    }

    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun saveCurrentLocation(data: MemberGroupId): MemberLocationRaw {
        val memberKey = cryptoKEKRepo.getMemberSenderKey(
            groupId = data.groupId,
            memberUserId = appPrefs.userId,
            locationId = data.locationId
        )
        if (memberKey !is GroupMemberSenderKey.Valid)
            throw MissingMemberKeyException(data.groupId, appPrefs.userId)

        val locationData = data.locationData?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.key
            )
        }

        val status = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(data.deviceStatus),
            key = memberKey.key
        )

        val payload = mapOf(
            "locationData" to locationData,
            "status" to status,
            "deviceKeyVersion" to memberKey.deviceKeyVersion,
        ).filter { it.value != null }

        return documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = payload
        ).let { MemberLocationRaw.from(it) }
    }

    suspend fun getMember(memberId: String): MemberWithLocationsRaw {
        return blocking {

            val member = documentApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersID,
                documentId = memberId
            ).let { MemberDocumentRaw.from(it) }

            val settings = if (member.ownerUserId == appPrefs.userId) {
                documentApi.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = remoteConfig.resourceIds.collectionGroupSettingsID,
                    documentId = memberId
                ).let { GroupSettingsRaw.from(it) }
            } else {
                null
            }

            val locations = documentApi.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = member.groupId))
            ).map { MemberLocationRaw.from(it) }

            MemberWithLocationsRaw(
                member = member.copy(settingsRaw = settings),
                locations = locations.filter { location -> member.locationIds.contains(location.id) }
            )
        }
    }
}