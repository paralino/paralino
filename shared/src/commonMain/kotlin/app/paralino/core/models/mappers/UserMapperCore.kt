package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.asImageModel
import app.paralino.core.models.raw.BlocklistRaw
import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.BlockListModelData
import app.paralino.core.models.user.BlocklistModel
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.isoWithOffsetToInstant

class UserMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val appPrefs: AppPrefs,
) {

    suspend fun remote2Core(model: BlocklistRaw): BlocklistModel {
        return BlocklistModel(
            id = model.id,
            ownerUserId = model.ownerUserId,
            resourceId = model.resourceId,
            resourceType = BlocklistType.fromId(model.resourceType),
            data = model.data?.let {
                try {
                    cryptoCore.decryptDataWithKey(
                        data = it,
                        key = userKeyRepo.personalKey(version = model.userKeyVersion).key
                    ).let {
                        BaseRemoteModule.formatter.decodeFromString<BlockListModelData>(it)
                    }.let {
                        CryptoResource.Ready(it)
                    }
                } catch (e: Exception) {
                    CryptoResource.WrongKeyOrData
                }
            }
        )
    }

    suspend fun remote2Core(model: UserMeRaw): UserMe {
        return UserMe(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            emailVerificationSkipTime = appPrefs.emailVerificationSkipTime,
            passwordUpdate = model.passwordUpdate?.isoWithOffsetToInstant(),
            data = model.data?.let { remote2Core(it) }
        )
    }

    suspend fun remote2Core(model: UserMeDataRaw): UserMeData {
        val userKey = userKeyRepo.personalKey(version = model.userKeyVersion)
        return UserMeData(
            userImage = model.userImage?.let {
                try {
                    cryptoCore.decryptDataWithKey(it, userKey.key)
                } catch (e: Exception) {
                    null
                }
            }?.asImageModel(),
            userName = model.userName?.let {
                try {
                    cryptoCore.decryptDataWithKey(it, userKey.key)
                } catch (e: Exception) {
                    ""
                }
            } ?: "",
        )
    }

    suspend fun core2Remote(model: UserMeData): UserMeDataRaw {
        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        return UserMeDataRaw(
            userImage = model.userImage?.image?.let {
                cryptoCore.encryptDataWithKey(it, userKey.key)
            },
            userName = model.userName.takeIf { it.isNotBlank() }?.let {
                cryptoCore.encryptDataWithKey(it, userKey.key)
            },
            userKeyVersion = userKey.version
        )
    }
}