package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
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
        ) ?: throw MissingGroupKeyException(model.id, model.keyVersion)

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

    suspend fun remote2Core(model: GroupDocumentRaw, memberCount: Int = 0): UserGroup {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            ownerPublicKey = model.ownerPublicKey,
            keyVersion = model.keyVersion
        ) ?: throw MissingGroupKeyException(model.id, model.keyVersion)

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
            memberCount = memberCount
        )
    }

    suspend fun remote2CoreWithMembers(model: GroupWithMemberRaw): UserGroupWithMembers {
        val members = model.members.map {
            memberMapperCore.remote2Core(
                model = it,
                groupOwnerPublicKey = model.group.ownerPublicKey
            )
        }
        return UserGroupWithMembers(
            group = remote2Core(model.group, members.size),
            members = members
        )
    }
}