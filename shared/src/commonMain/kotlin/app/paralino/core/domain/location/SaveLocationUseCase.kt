package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException

class SaveLocationUseCase(
    private val local: LocationLocalDataSource,
    private val documentApi: IDocumentsApi,
    private val appBuildConfig: AppBuildConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val appPrefs: AppPrefs
) {
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

        val newData = documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = payload
        ).let { MemberLocationRaw.from(it) }

        local.createOrUpdateMemberLocation(newData)

        return newData
    }
}