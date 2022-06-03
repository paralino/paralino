package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.exceptions.MissingGroupKeyException
import com.zbapp.core.utils.exceptions.MissingMemberKeyException
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
            ) ?: throw MissingGroupKeyException(model.id)
        } ?: memberKey

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.data.content
            )
        }

        // data encrypted with group DEK
        val memberData = model.data.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = groupKey.data.content
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
        ) ?: throw MissingGroupKeyException(model.groupId)

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.takeIf { it.isNotBlank() }?.let {
            val data = cryptoCore.decryptDataWithKey(
                data = it,
                key = memberKey?.data?.content
                    ?: throw MissingMemberKeyException(model.groupId, model.ownerUserId)
            )
            data.takeIf { it.isNotBlank() }?.let {
                BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(it)
            }
        }

        // data encrypted with group DEK
        val memberData = model.data.let {
            val data = cryptoCore.decryptDataWithKey(
                data = it,
                key = groupKey.data.content
            )
            BaseRemoteModule.formatter.decodeFromString<MemberModelData>(data)
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