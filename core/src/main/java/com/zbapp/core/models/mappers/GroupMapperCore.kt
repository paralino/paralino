package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.NotFoundException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class GroupMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val memberMapperCore: MemberMapperCore
) {
    suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw NotFoundException("Missing group key")

        val groupData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(model.data),
            key = groupKey.data.content
        )

        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            data = groupData
        )
    }

    suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw NotFoundException("Missing group key")

        val groupData = cryptoCore.decryptDataWithKey(
            data = model.data,
            key = groupKey.data.content
        ).let {
            BaseRemoteModule.formatter.decodeFromString<UserGroupData>(it)
        }

        return UserGroup(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            data = groupData,
            members = emptyList()
        )
    }

    suspend fun remote2CoreWithMembers(model: GroupWithMemberRaw): UserGroup {
        val members = model.members.map {
            memberMapperCore.remote2Core(
                model = it,
                groupOwnerPublicKey = model.group.ownerPublicKey
            )
        }
        return remote2Core(model.group).copy(members = members)
    }
}