package app.paralino.core.repo.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.*

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val placesRepo: PlacesRepository,
    private val groupRefreshUseCase: GroupRefreshUseCase
) : BaseRepository() {

    fun getAllGroupsLocally(): Flow<List<UserGroup>> {
        return local.getAllGroups()
    }

    fun getAllGroupIds(): Flow<List<String>> {
        return local.getAllGroupIds()
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroupWithMembers> {
        return local.getGroupWithMembers(id)
    }

    fun getGroupWithoutMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithoutMembers(id)
    }

    suspend fun scheduleRefreshAllGroupsWithPlaces(
        key: String = "groups_refresh",
        time: Long = 15_000
    ) {
        getDataOnce(
            local = flowOf(key),
            remotePreference = RemotePreference.CACHE(key = key, timeMillis = time)
        ) {
            refreshAllGroupsWithPlaces()
        }
            .catch { }
            .collect()
    }

    private suspend fun refreshAllGroupsWithPlaces() {
        refreshAllGroups()
        placesRepo.refreshAllPlaces()
    }

    private suspend fun refreshAllGroups() {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key").w("refreshing all groups")
            remote.getAllGroups()
                .also { cryptoKEKRepo.refreshAllKeys().first() }
                .let { it.map { groupRefreshUseCase.handleGroupKeysUpdate(it) } }
        }
            .also {
                logg.tag("tag_crypto_key_group").d("group and keys ready, updating groups")
                local.recreateGroups(it)
            }
    }

    fun updateGroup(userGroup: UserGroup): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updateGroup(userGroup) },
            localUpdate = { local.updateGroupOnly(it) }
        ).map { Unit }
    }

    suspend fun clear() {
        local.clearAll()
    }

}