package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
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
            keyVersion = model.groupKeyVersion
        )
        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(model.id, model.groupKeyVersion)

        val groupData = when (model.data) {
            is CryptoResource.Ready -> {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(model.data.data),
                    key = groupKey.key
                )
            }

            else -> throw CryptoException("Group data is missing: ${model.id}")
        }

        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            data = groupData,
            serverIntegrity = model.serverIntegrity,
            planType = model.planType.key,
            schemeVersion = model.schemeVersion,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride
        )
    }

    suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.id,
            groupOwnerId = model.ownerUserId,
            keyVersion = model.groupKeyVersion
        )

        if (groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.id, model.groupKeyVersion))

        val groupData = if (groupKey is GroupMasterKey.Valid) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = groupKey.key
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
            groupKeyVersion = model.groupKeyVersion,
            data = groupData,
            serverIntegrity = model.serverIntegrity,
            planType = PlanType.fromKey(model.planType) ?: PlanType.default,
            schemeVersion = model.schemeVersion,
            membersLimitOverride = model.membersLimitOverride,
            devicesLimitOverride = model.devicesLimitOverride,
            placesLimitOverride = model.placesLimitOverride
        )
    }

    suspend fun remote2Core(
        model: GroupDocumentRaw,
        memberCount: Int = 0,
        verification: VerificationResult
    ): UserGroupWithMetadata {
        return UserGroupWithMetadata(
            group = remote2Core(model),
            memberCount = memberCount,
            verification = verification,
        )
    }

    suspend fun remote2CoreWithMembers(model: GroupWithMembersRaw): UserGroupWithMembers {
        val group = remote2Core(model.group)
        val members = model.members.map {
            memberMapperCore.remote2Core(
                model = it,
                group = group
            )
        }
        return UserGroupWithMembers(
            group = group,
            members = members
        )
    }
}