package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GroupRemoveUseCase(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placesRepo: PlacesRepository,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val locationSharingManager: LocationSharingManager,
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
                remote.removeUserFromGroup(groupId, groupOwnerId!!, userRepository.cachedUserId)
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
        locationSharingManager.stopSharing(groupId)
        local.deleteGroup(groupId)
        placesRepo.deleteLocallyForGroup(groupId)
        cryptoKEKRepo.deleteAllKeysForGroup(groupId)
    }
}