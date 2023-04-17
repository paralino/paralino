package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.asImageModel
import app.paralino.core.models.raw.BlocklistRaw
import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.*
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString

class UserMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
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
                        key = userKeyRepo.personalKey
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
            data = model.data?.let { remote2Core(it) }
        )
    }

    suspend fun remote2Core(model: UserMeDataRaw): UserMeData {
        val userKey = userKeyRepo.personalKey
        return UserMeData(
            userImage = model.userImage?.let { cryptoCore.decryptDataWithKey(it, userKey) }
                ?.asImageModel(),
            userName = model.userName?.let { cryptoCore.decryptDataWithKey(it, userKey) } ?: "",
        )
    }

    suspend fun core2Remote(model: UserMeData): UserMeDataRaw {
        val userKey = userKeyRepo.personalKey
        return UserMeDataRaw(
            userImage = model.userImage?.image?.let { cryptoCore.encryptDataWithKey(it, userKey) },
            userName = model.userName.takeIf { it.isNotBlank() }
                ?.let { cryptoCore.encryptDataWithKey(it, userKey) },
        )
    }

}