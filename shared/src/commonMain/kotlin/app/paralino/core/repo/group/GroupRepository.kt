package app.paralino.core.repo.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val memberLocal: ILocationLocalDataSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val placesRepo: PlacesRepository,
    private val groupRefreshUseCase: GroupRefreshUseCase,
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase
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

    suspend fun scheduleRefreshAllGroupsWithPlaces(key: String = "groups_refresh", time: Long = 15_000) {
        getDataOnce(
            local = flowOf(key),
            remotePreference = RemotePreference.CACHE(key = key, timeMillis = time)
        ) {
            refreshAllGroupsWithPlaces()
        }
            .catch {  }
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

    fun createGroup(name: String, image: ImageModel): Flow<Unit> {
        return getDataOnce(
            remote = {
                val userData = userRepository.cachedUser!!.data!!
                remote.createGroup(
                    name = name,
                    image = image,
                    myData = MemberModelData(userData.userName, userData.userImage),
                    myKey = userKeyRepo.latestKeyPair()
                ).also { cryptoKEKRepo.refreshKeysForGroup(it.group.id).first() }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        ).map { Unit }
    }

    fun updateGroup(userGroup: UserGroup): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updateGroup(userGroup) },
            localUpdate = { local.updateGroupOnly(it) }
        ).map { Unit }
    }

    suspend fun gotPlaceUpdateFromRealtime(place: GroupPlaceRaw) {
        // if don't update place if group actions are processing
        // place will be updated after everything is finished
        if(!groupPreRefreshUseCase.isProcessing(place.groupId))
            placesRepo.updatePlaceLocally(place)
    }

    suspend fun gotMemberDeleteFromRealtime(groupId: String) {
        logg.tag("realtime_events").v("Delete member event in group $groupId")
        groupPreRefreshUseCase.refreshGroupWithMembers(groupId)
    }

    suspend fun gotMemberUpdateFromRealtime(member: MemberDocumentRaw) {
        val memberKey = try {
            cryptoKEKRepo.getMemberKey(groupId = member.groupId, memberUserId = member.ownerUserId, memberPublicKey = member.ownerPublicKey)
        } catch (e: Exception) {
            null
        }

        if(memberKey == null || member.memberKeyVersion != memberKey.version) {
            logg.tag("realtime_events").v("Refresh group - update member (${member.id}) event in group ${member.groupId}")
            // this is either new member so we are missing keys, or keys are obsolete
            groupPreRefreshUseCase.refreshGroupWithMembers(member.groupId)
        } else {
            logg.tag("realtime_events").v("Update member (${member.id}) event in group ${member.groupId}")
            memberLocal.createOrUpdateMembers(member)
        }
    }

    suspend fun clear() {
        local.clearAll()
    }

}