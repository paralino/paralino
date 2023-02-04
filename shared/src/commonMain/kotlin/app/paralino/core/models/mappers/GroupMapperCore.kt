package app.paralino.core.models.mappers

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
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
            groupOwnerId = model.ownerUserId,
            keyVersion = model.keyVersion
        ) ?: throw MissingGroupKeyException(model.id, model.keyVersion)

        val groupData = when (model.data) {
            is CryptoResource.Ready -> {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(model.data.data),
                    key = groupKey.data.content
                )
            }
            else -> throw CryptoException("Group data is missing: ${model.id}")
        }

        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            data = groupData
        )
    }

    suspend fun remote2Core(model: GroupDocumentRaw, memberCount: Int = 0, verification: VerificationResult): UserGroup {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            groupOwnerId = model.ownerUserId,
            keyVersion = model.keyVersion
        )

        if(groupKey == null)
            logg.e(MissingGroupKeyException(model.id, model.keyVersion))

        val groupData = if (groupKey != null) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = groupKey.data.content
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<UserGroupData>(it)
                }.let {
                    CryptoResource.Ready(it)
                }
            } catch (e: Exception) {
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.MissingKey
        }

        return UserGroup(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            data = groupData,
            memberCount = memberCount,
            verification = verification
        )
    }

    suspend fun remote2CoreWithMembers(model: GroupWithMemberRaw): UserGroupWithMembers {
        val members = model.members.map {
            memberMapperCore.remote2Core(
                model = it,
                groupOwnerId = model.group.ownerUserId
            )
        }
        val verification = members.first { it.member.userId == config.userId }.member.verificationResult
        return UserGroupWithMembers(
            group = remote2Core(model.group, members.size, verification),
            members = members
        )
    }
}