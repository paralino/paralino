package app.paralino.core.repo.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.location.LocationDataModel
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
    private val appBuildConfig: AppBuildConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val appPrefs: AppPrefs
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

    suspend fun clearLocation(
        groupId: String,
        locationId: String
    ) {
        // passing fake data here so we hide the fact that users cleared their location
        blocking {
            val memberKey = cryptoKEKRepo.getMemberSenderKey(
                groupId = groupId,
                memberUserId = appPrefs.userId,
                locationId = locationId
            )
            if (memberKey !is GroupMemberSenderKey.Valid)
                throw MissingMemberKeyException(groupId, appPrefs.userId)

            val payload = LocationDataModel(
                lat = 0.0,
                lng = 0.0,
                accuracy = 10000000f,
                timeUTC = "2021-01-17T21:47:27+01:00",
                endTimeUTC = null,
                speed = null,
                altitude = null,
                isMasked = true
            )

            val locationData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(payload),
                key = memberKey.key
            )

            documentApi.updateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                docId = locationId,
                data = mapOf(
                    "locationData" to locationData,
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

        val locationHistory = data.locationHistory?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.locationHistoryFormatter.encodeToString(it),
                key = memberKey.key
            )
        }

        val status = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(data.deviceStatus),
            key = memberKey.key
        )

        val payload = mapOf(
            "locationData" to locationData,
            "locationHistory" to locationHistory,
            "status" to status,
            "deviceKeyVersion" to memberKey.deviceKeyVersion,
        ).filter { it.value != null }

        return documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = payload
        ).let { MemberLocationRaw.from(it) }
    }

    suspend fun getMember(memberId: String): MemberWithLocationsRaw {
        return blocking {

            val member = documentApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appBuildConfig.resourceIds.collectionGroupMembersID,
                documentId = memberId
            ).let { MemberDocumentRaw.from(it) }

            val settings = if (member.ownerUserId == appPrefs.userId) {
                documentApi.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupSettingsID,
                    documentId = memberId
                ).let { GroupSettingsRaw.from(it) }
            } else {
                null
            }

            val locations = documentApi.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = member.groupId))
            ).map { MemberLocationRaw.from(it) }

            MemberWithLocationsRaw(
                member = member.copy(settingsRaw = settings),
                locations = locations.filter { location -> member.locationIds.contains(location.id) }
            )
        }
    }
}