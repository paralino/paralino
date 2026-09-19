package app.paralino.core.domain.group

import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlaceActivityDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GroupRemoveUseCase(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val locationSharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs,
    private val pendingGroupJoinWelcome: PendingGroupJoinWelcomeUseCase,
) : LimitedExecutionRepository() {

    fun deleteGroup(groupId: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.deleteGroup(groupId)
            },
            localUpdate = {
                clearGroupLocally(groupId)
            }
        )
            .startProcessing(groupId)
    }

    fun leaveGroup(groupId: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val groupOwnerId = local.getGroupOwnerId(groupId)
                remote.removeUserFromGroup(
                    groupId = groupId,
                    groupOwnerId = groupOwnerId!!,
                    memberUserId = appPrefs.userId
                )
            },
            localUpdate = {
                clearGroupLocally(groupId)
            }
        )
            .startProcessing(groupId)
    }

    fun blockGroup(groupId: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = local.getGroupWithoutMembers(groupId).first()
                remote.blockGroup(group)
            },
            localUpdate = {
                clearGroupLocally(groupId)
            }
        )
            .startProcessing(groupId)
    }

    suspend fun clearGroupLocally(groupId: String) {
        locationSharingManager.stopSharing(groupId = groupId.asGroupOrSpaceId(), deleteContract = true)
        local.deleteGroup(groupId = groupId)
        placeActivityRepo.clearForGroup(groupId = groupId.asGroupOrSpaceId())
        cryptoKEKRepo.deleteAllKeysForGroup(groupId = groupId)
        pendingGroupJoinWelcome.delete(groupId = groupId)
    }
}