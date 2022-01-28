package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.crypto.CryptoKeyRepository
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.base.IGroupCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.NotFoundException
import com.zbapp.locationtrack.data.models.location.MemberEntity
import kotlinx.serialization.decodeFromString
import java.util.*

class MemberMapper(
    private val crypto: IGroupCrypto,
    private val cryptoRepo: CryptoKeyRepository,
    private val cryptoAuth: CryptoAuth
) : IMemberMapperCore, IDataMapperLocal<MemberEntity, MemberDocumentRaw> {

    override suspend fun core2Remote(model: MemberModel): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = "",
            data = crypto.encryptModel(model.groupId, model.data) ?: "",
            mutableData = crypto.encryptModel(groupId = model.id, data = model.data)
        )
    }

    override suspend fun remote2Core(model: MemberDocumentRaw): MemberModel {
        // TODO move to crypto repository
        val memberKey = cryptoRepo.getMemberKey(model.groupId ?: model.id, model.id)?.let {
            cryptoAuth.decryptDataWithKeyPair(
                data = it.encryptedSenderKey,
                myPrivateKey = "", // TODO use user private key (KEK)
                publicKey = model.ownerPublicKey
            )
        }
            ?: throw NotFoundException("Missing user key")

        val groupKey = model.groupId?.let {
            cryptoRepo.getGroupKey(it)?.let {
                cryptoAuth.decryptDataWithKeyPair(
                    data = it.encryptedGroupKey,
                    myPrivateKey = "", // TODO use user private key (KEK)
                    publicKey = "" // TODO verify with group owner public key
                )
            } ?: throw NotFoundException("Missing group key")
        } ?: memberKey

        // data encrypted with user senderKey DEK
        val locationData = model.mutableData?.let {
            val data = cryptoAuth.decryptDataWithKey(
                data = it,
                key = memberKey
            )
            BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(data)
        }

        // data encrypted with group DEK
        val memberData = model.data.let {
            val data = cryptoAuth.decryptDataWithKey(
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

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = model.ownerPublicKey,
            data = model.data,
            mutableData = model.mutableData
        )
    }

    override suspend fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerUserId,
            groupId = model.groupId,
            ownerPublicKey = model.ownerPublicKey,
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
