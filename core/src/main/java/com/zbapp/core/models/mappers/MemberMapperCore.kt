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
            memberId = model.id,
            memberPublicKey = model.publicKey
        ) ?: throw NotFoundException("Missing user key")

        val groupKey = model.groupId?.let {
            cryptoKEKRepo.getGroupKey(
                groupId = it,
                ownerPublicKey = groupOwnerPublicKey
            ) ?: throw NotFoundException("Missing group key")
        } ?: memberKey

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey
            )
        }

        // data encrypted with group DEK
        val memberData = model.data.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = groupKey
            )
        }

        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = model.publicKey,
            data = memberData,
            mutableData = locationData
        )
    }

    suspend fun remote2Core(model: MemberDocumentRaw, groupOwnerPublicKey: String): MemberModel {

        val memberKey = cryptoKEKRepo.getMemberKey(
            groupId = model.groupId ?: model.id,
            memberId = model.id,
            memberPublicKey = model.ownerPublicKey
        ) ?: throw NotFoundException("Missing user key")

        val groupKey = model.groupId?.let {
            cryptoKEKRepo.getGroupKey(
                groupId = it,
                ownerPublicKey = groupOwnerPublicKey
            ) ?: throw NotFoundException("Missing group key")
        } ?: memberKey

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.let {
            val data = cryptoCore.decryptDataWithKey(
                data = it,
                key = memberKey
            )
            BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(data)
        }

        // data encrypted with group DEK
        val memberData = model.data.let {
            val data = cryptoCore.decryptDataWithKey(
                data = it,
                key = groupKey
            )
            BaseRemoteModule.formatter.decodeFromString<MemberModelData>(data)
        }

        return MemberModel(
            id = model.id,
            groupId = model.groupId ?: model.id,
            userId = model.ownerUserId,
            data = memberData,
            mutableData = locationData,
            publicKey = model.ownerPublicKey
        )
    }
}