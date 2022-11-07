package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class MemberMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) {

    suspend fun core2Remote(model: MemberModel, groupOwnerPublicKey: String): MemberDocumentRaw {

        val memberKey = cryptoKEKRepo.getMemberKey(
            groupId = model.groupId ?: model.id,
            memberUserId = model.userId,
            memberPublicKey = model.publicKey
        ) ?: throw MissingMemberKeyException(model.groupId, model.userId)

        val groupKey = model.groupId.let {
            cryptoKEKRepo.getGroupKey(
                groupId = it,
                ownerPublicKey = groupOwnerPublicKey,
                // use latest group key
                keyVersion = null
            ) ?: throw MissingGroupKeyException(model.id, keyVersion = null)
        }

        // data encrypted with group DEK
        val memberData = when (model.data) {
            is CryptoResource.Ready -> {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(model.data.data),
                    key = groupKey.data.content
                )
            }
            else -> throw CryptoException("Member data is missing: ${model.id}")
        }

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData.data?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.data.content
            )
        }

        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = model.publicKey,
            groupKeyVersion = cryptoKEKRepo.getLatestGroupKeyVersion(model.groupId)!!,
            memberKeyVersion = model.memberKeyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            data = memberData,
            mutableData = locationData
        )
    }

    suspend fun remote2Core(model: MemberDocumentRaw, groupOwnerPublicKey: String): MemberModel {

        val memberKey = cryptoKEKRepo.getMemberKey(
            groupId = model.groupId,
            memberUserId = model.ownerUserId,
            memberPublicKey = model.ownerPublicKey
        )

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.groupId,
            ownerPublicKey = groupOwnerPublicKey,
            keyVersion = model.groupKeyVersion
        )

        if(groupKey == null)
            logg.e(MissingGroupKeyException(model.id, model.groupKeyVersion))

        // data encrypted with user senderKey DEK
        val locationData = if(memberKey != null) {
            if(model.mutableData.isNullOrBlank()) {
                CryptoResource.NotInitialized
            } else {
                try {
                    cryptoCore.decryptDataWithKey(
                        data = model.mutableData,
                        key = memberKey.data.content
                    ).let {
                        BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(it)
                    }.let {
                        CryptoResource.Ready(it)
                    }
                } catch (e: Exception) {
                    logg.tag("tag_crypto_key_group_member")
                        .e("Error decrypting location data for user ${model.ownerUserId} - member ${model.id} in group ${model.groupId}\n" +
                                "Model key version: ${model.memberKeyVersion} - key version: ${memberKey.version}", e)
                    CryptoResource.WrongKeyOrData
                }
            }
        } else {
            CryptoResource.MissingKey
        }


        // data encrypted with group DEK
        val memberData = if(groupKey != null) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = groupKey.data.content
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<MemberModelData>(it)
                }.let {
                    CryptoResource.Ready(it)
                }
            } catch (e: Exception) {
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.MissingKey
        }

        return MemberModel(
            id = model.id,
            groupId = model.groupId,
            userId = model.ownerUserId,
            publicKey = model.ownerPublicKey,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            data = memberData,
            mutableData = locationData
        )
    }
}