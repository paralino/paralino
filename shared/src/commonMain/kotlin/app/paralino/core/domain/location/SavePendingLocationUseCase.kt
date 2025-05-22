package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.location.PendingLocationHistoryEntity
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import kotlin.coroutines.cancellation.CancellationException

class SavePendingLocationUseCase(
    private val database: MainDatabaseHelper,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeysRepo: CryptoUserKeysLocalSource,
    private val appBuildConfig: AppBuildConfig,
    private val appPrefs: AppPrefs
) {
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun saveNewLocations(data: List<MemberGroupId>) {
        val userKey = cryptoUserKeysRepo.activePersonalKeyWithVersion()

        database.pendingLocationHistoryDao.createEntries(
            entries = data.map {
                val locationHistoryData = it.locationHistory?.let {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.locationHistoryFormatter.encodeToString(it),
                        key = userKey.key
                    )
                }

                PendingLocationHistoryEntity(
                    id = 0,
                    groupId = it.groupId,
                    memberId = it.memberId,
                    locationId = it.locationId,
                    locationHistoryData = locationHistoryData,
                    userKeyVersion = userKey.version
                )
            }
        )
    }
}