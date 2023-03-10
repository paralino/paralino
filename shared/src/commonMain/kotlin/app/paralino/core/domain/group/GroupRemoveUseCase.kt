package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupRemoveUseCase(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placesRepo: PlacesRepository,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val locationSharingManager: LocationSharingManager,
): BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())

    fun deleteGroup(groupId: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.deleteGroup(groupId)
            },
            localUpdate = {
                clearGroupLocally(groupId)
            }
        )
            .onStart { setAsProcessing(groupId) }
            .onEach { stopProcessing(groupId) }
            .catch {
                stopProcessing(groupId)
                throw it
            }
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
            .onStart { setAsProcessing(groupId) }
            .onEach { stopProcessing(groupId) }
            .catch {
                stopProcessing(groupId)
                throw it
            }
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
            .onStart { setAsProcessing(groupId) }
            .onEach { stopProcessing(groupId) }
            .catch {
                stopProcessing(groupId)
                throw it
            }
    }

    suspend fun clearGroupLocally(groupId: String) {
        locationSharingManager.stopSharing(groupId)
        local.deleteGroup(groupId)
        placesRepo.deleteLocallyForGroup(groupId)
        cryptoKEKRepo.deleteAllKeysForGroup(groupId)
    }

}