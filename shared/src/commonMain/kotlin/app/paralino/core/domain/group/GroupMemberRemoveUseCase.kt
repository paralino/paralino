package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.location.MemberModel
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GroupMemberRemoveUseCase(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val memberRemote: LocationRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupRemoveUseCase: GroupRemoveUseCase,
    private val groupRefreshUseCase: GroupRefreshUseCase
) : LimitedExecutionRepository() {

    fun removeMember(groupId: String, memberUserId: String): Flow<Unit> {

        if (memberUserId == userRepository.cachedUserId) {
            return groupRemoveUseCase.leaveGroup(groupId)
        }

        return getDataOnce(
            remote = {
                val groupOwnerId = local.getGroupOwnerId(groupId)
                remote.removeUserFromGroup(groupId, groupOwnerId!!, memberUserId)
            },
            localUpdate = {
                local.removeMember(groupId, memberUserId)
                cryptoKEKRepo.deleteKeysForUser(groupId, memberUserId)
            }
        )
            .onEach {
                groupRefreshUseCase.refreshRemoteGroupWithMembersAndPlaces(groupId, cacheTimeMillis = 0)
            }
            .startProcessing(groupId)
    }

    fun deleteLocation(memberId: String, locationId: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                memberRemote.deleteLocation(memberId = memberId, locationId = locationId)
                memberRemote.getMember(memberId)
            },
            localUpdate = {
                local.updateOrCreateMember(it)
            }
        ).map { Unit }
    }

    fun blockUser(member: MemberModel): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.blockUser(member)
            },
            localUpdate = {
                userRepository.refreshBlocklist(cacheTimeMillis = 0)
            }
        ).map { Unit }
    }

}