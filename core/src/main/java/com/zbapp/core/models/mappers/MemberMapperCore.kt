package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.NotFoundException
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
        ) ?: throw NotFoundException("Missing user key")

        val groupKey = model.groupId?.let {
            cryptoKEKRepo.getGroupKey(
                groupId = it,
                ownerPublicKey = groupOwnerPublicKey,
                // use latest group key
                keyVersion = null
            ) ?: throw NotFoundException("Missing group key")
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
            groupId = model.groupId ?: model.id,
            memberUserId = model.ownerUserId,
            memberPublicKey = model.ownerPublicKey
        )

        val groupKey = model.groupId?.let {
            cryptoKEKRepo.getGroupKey(
                groupId = it,
                ownerPublicKey = groupOwnerPublicKey,
                keyVersion = model.groupKeyVersion
            ) ?: throw NotFoundException("Missing group key")
        } ?: memberKey ?: throw NotFoundException("Missing group key")

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.takeIf { it.isNotBlank() }?.let {
            val data = cryptoCore.decryptDataWithKey(
                data = it,
                key = memberKey?.data?.content ?: throw NotFoundException("Missing member key")
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
            groupId = model.groupId ?: model.id,
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