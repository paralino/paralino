package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class GroupRefreshUseCase constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placeRepo: PlacesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupMapperCore: GroupMapperCore,
    private val groupCryptoKeyRotation: GroupCryptoKeyRotation,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val groupRemove: GroupRemoveUseCase,
) : BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())

    fun getGroupWithMembers(id: String): Flow<UserGroupWithMembers> {
        return mergeDataWithRemote(
            local = local.getGroupWithMembers(id),
            remote = { refreshRemoteGroupWithMembersAndPlaces(id) },
            localUpdate = { }
        )
    }

    suspend fun refreshRemoteGroupWithMembersAndPlaces(groupId: String): GroupWithMemberRaw {
        return getRemoteGroupWithMembers(groupId)
            .also {
                local.updateOrCreateGroup(it)
            }.also {
                placeRepo.refreshPlacesForGroup(groupId)
            }
    }

    private suspend fun getRemoteGroupWithMembers(groupId: String): GroupWithMemberRaw {
        return try {
            triggerRemoteOnly {
                remote.getGroupWithMembers(groupId)
                    .also { cryptoKEKRepo.refreshKeysForGroup(groupId).first() }
                    .let { handleGroupKeysUpdate(it) }
            }
        } catch (e: Exception) {
            if (e.asRemoteException() is NotFoundException) {
                groupRemove.clearGroupLocally(groupId)
            }
            throw e
        }
    }

    suspend fun handleGroupKeysUpdate(group: GroupWithMemberRaw): GroupWithMemberRaw {
        logg.tag("tag_crypto_key_group").w("group and member keys updated")
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group)

        if (groupCryptoKeyRotation.rotateGroupAndMemberKeysIfNeeded(remoteGroup, force = false)) {
            return getRemoteGroupWithMembers(remoteGroup.id)
        } else if (memberCryptoKeyRotation.rotateMemberKeysIfNeeded(remoteGroup, force = false)) {
            return getRemoteGroupWithMembers(remoteGroup.id)
        } else {
            return group
        }
    }

    suspend fun updateGroupAndMemberKeysAndRefresh(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
    ): GroupWithMemberRaw? {
        if (groupCryptoKeyRotation.rotateGroupAndMemberKeysIfNeeded(remoteGroup, force)) {
            return refreshRemoteGroupWithMembersAndPlaces(remoteGroup.id)
        } else {
            return null
        }
    }

}