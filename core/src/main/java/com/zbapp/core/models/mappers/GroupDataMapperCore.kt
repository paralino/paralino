package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.crypto.CryptoKeyRepository
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.NotFoundException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class GroupDataMapperCore constructor(
    private val cryptoRepo: CryptoKeyRepository,
    private val cryptoAuth: CryptoAuth
) {
    suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {

        val groupKey = cryptoRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey
        ) ?: throw NotFoundException("Missing group key")

        val groupData = cryptoAuth.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(model.data),
            key = groupKey
        )

        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = groupData
        )
    }

    suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {

        val groupKey = cryptoRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey
        ) ?: throw NotFoundException("Missing group key")

        val groupData = cryptoAuth.decryptDataWithKey(
            data = model.data,
            key = groupKey
        ).let {
            BaseRemoteModule.formatter.decodeFromString<UserGroupData>(it)
        }

        return UserGroup(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = groupData,
            members = emptyList(),
            groupKey = ""
        )
    }
}