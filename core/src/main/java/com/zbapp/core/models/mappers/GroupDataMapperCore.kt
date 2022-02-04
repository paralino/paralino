package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.NotFoundException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class GroupDataMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore
) {
    suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw NotFoundException("Missing group key")

        val groupData = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(model.data),
            key = groupKey
        )

        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
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
            key = groupKey
        ).let {
            BaseRemoteModule.formatter.decodeFromString<UserGroupData>(it)
        }

        return UserGroup(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion,
            data = groupData,
            members = emptyList()
        )
    }
}