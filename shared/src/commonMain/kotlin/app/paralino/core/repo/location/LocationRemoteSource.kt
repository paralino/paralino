package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.*
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException

class LocationRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userMemberIdUseCase: UserMemberIdUseCase
) {

    suspend fun deleteLocation(memberId: String, locationId: String) {
        blocking {

            val payload = LocationDeletePayload(
                memberId = memberId,
                locationId = locationId
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionLocationDelete,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to delete location",
                    Exception(response.stderr)
                )
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

    suspend fun saveCurrentLocation(
        ids: List<MemberGroupId>,
    ) {
        blocking {
            ids.forEach {
                try {
                    saveCurrentLocation(it)
                } catch (e: Exception) {
                    logg.e("Error saving location for group ${it.groupId}", e)
                }
            }
        }
    }

    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun saveCurrentLocation(
        data: MemberGroupId,
    ) {
        val memberKey = cryptoKEKRepo.getMemberSenderKey(
            groupId = data.groupId,
            memberUserId = config.userId,
            locationId = data.locationId
        )
        if (memberKey !is GroupMemberSenderKey.Valid)
            throw MissingMemberKeyException(data.groupId, config.userId)

        val groupKey = cryptoKEKRepo.getGroupKey(data.groupId, data.member.groupKeyVersion)
        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(data.groupId)

        val locationData = data.data?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.key
            )
        }

        val deviceData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(
                MemberDeviceData(
                    deviceName = config.deviceName,
                    deviceType = userMemberIdUseCase.getCurrentDeviceType().key
                )
            ),
            key = groupKey.key
        )

        documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = mapOf(
                "locationData" to locationData,
                "deviceData" to deviceData,
                "groupKeyVersion" to groupKey.version,
                "deviceKeyVersion" to memberKey.deviceKeyVersion,
            )
        )
    }


    suspend fun getMember(memberId: String): MemberWithLocationsRaw {
        return blocking {

            val member = documentApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersID,
                documentId = memberId
            ).let { MemberDocumentRaw.from(it) }

            val settings = if (member.ownerUserId == config.userId) {
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
                keyValues = listOf("groupId" to member.groupId)
            ).map { MemberLocationRaw.from(it) }

            MemberWithLocationsRaw(
                member = member.copy(settingsRaw = settings),
                locations = locations.filter { location -> member.locationIds.contains(location.id) }
            )
        }
    }

}