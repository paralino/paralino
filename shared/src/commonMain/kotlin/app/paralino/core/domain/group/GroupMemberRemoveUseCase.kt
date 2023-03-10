package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.location.MemberModel
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupMemberRemoveUseCase(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val memberRemote: LocationRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupRemoveUseCase: GroupRemoveUseCase,
    private val groupRefreshUseCase: GroupRefreshUseCase
): BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())

    fun removeMember(groupId: String, memberUserId: String): Flow<Unit> {

        if (memberUserId == userRepository.cachedUserId) {
            return groupRemoveUseCase.leaveGroup(groupId)
        }

        if(isProcessing(groupId)) {
            logg.tag("group_actions").v("Group member remove in process for $groupId")
            return flow { throw Exception("Remove member for group $groupId is still processing") }
        }

        logg.tag("group_actions").v("No group member remove in process, can remove member for group $groupId")

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
            .onStart {
                logg.tag("group_actions").v("Start processing group member remove for group $groupId")
                setAsProcessing(groupId)
            }
            .onEach {
                val group = local.getGroupWithMembers(groupId).first()
                groupRefreshUseCase.updateGroupAndMemberKeysAndRefresh(group)
                    .also {
                        logg.tag("group_actions").v("Stop processing group member remove for group $groupId")
                        stopProcessing(groupId)
                    }
            }
            .catch {
                stopProcessing(groupId)
                throw it
            }
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
                userRepository.refreshBlocklist()
            }
        ).map { Unit }
    }

}