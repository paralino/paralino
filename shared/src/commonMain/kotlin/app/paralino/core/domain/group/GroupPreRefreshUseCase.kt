package app.paralino.core.domain.group

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull

class GroupPreRefreshUseCase constructor(
    private val groupCryptoKeyRotation: GroupCryptoKeyRotation,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val deviceKeyRotation: MemberDeviceKeyRotation,
    private val groupRemove: GroupRemoveUseCase,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupMemberAdd: GroupMemberAddUseCase,
    private val groupRefresh: GroupRefreshUseCase,
    private val refreshEverything: RefreshEverythingUseCase,
    private val local: UserGroupLocalDataSource,
) : LimitedExecutionRepository() {

    override fun isProcessing(key: String): Boolean {
        val selfProcessing = super.isProcessing(key)
        val fullProcessing = refreshEverything.isProcessingAny()
        val groupRemoveProcessing = groupRemove.isProcessing(key)
        val groupMemberRemoveProcessing = groupMemberRemove.isProcessing(key)
        val groupMemberAddProcessing = groupMemberAdd.isProcessing(key)
        val groupCryptoKeyRotationProcessing = groupCryptoKeyRotation.isProcessing(key)
        val memberCryptoKeyRotationProcessing = memberCryptoKeyRotation.isProcessing(key)
        val deviceKeyRotationProcessing = deviceKeyRotation.isProcessing(key)

        val processing = selfProcessing || fullProcessing
                || groupRemoveProcessing || groupMemberRemoveProcessing || groupMemberAddProcessing
                || groupCryptoKeyRotationProcessing || memberCryptoKeyRotationProcessing || deviceKeyRotationProcessing

        logg.tag("group_actions").v(
            "Group pre-refresh processing ($processing):" +
                    " self: $selfProcessing" +
                    " full: $fullProcessing" +
                    " groupRemove: $groupRemoveProcessing" +
                    " groupMemberRemove: $groupMemberRemoveProcessing" +
                    " groupAddRemove: $groupMemberAddProcessing" +
                    " groupKeyRotation: $groupCryptoKeyRotationProcessing" +
                    " memberKeyRotation: $memberCryptoKeyRotationProcessing" +
                    " deviceKeyRotation: $deviceKeyRotationProcessing"
        )

        return processing
    }

    suspend fun refreshGroupWithMembers(groupId: String, cacheTimeMillis: Long = 6_000): Boolean {

        try {
            startProcessing(groupId)
        } catch (e: Exception) {
            logg.tag("group_actions").v("Processing, can't refresh group $groupId")
            return false
        }

        logg.tag("group_actions").v("Not processing, can refresh group $groupId")

        try {
            val result = triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(key = "group_refresh_$groupId", timeMillis = cacheTimeMillis),
                remote = { groupRefresh.refreshRemoteGroupWithMembersAndPlaces(groupId) }
            )
            return result != null
        } catch (e: Exception) {
            logg.tag("group_actions")
                .w("Stop processing group refresh for group $groupId - ${e.message}")
            return false
        } finally {
            stopProcessing(groupId)
        }
    }

    fun getAndRefreshGroupWithMembers(groupId: String): Flow<UserGroupWithMembers> {
        return mergeDataWithRemote(
            local = local.getGroupWithMembers(groupId = groupId)
                .catch { emit(null) }
                .filterNotNull(),
            remote = { refreshGroupWithMembers(groupId = groupId, cacheTimeMillis = 6000) },
            localUpdate = { },
            remotePreference = RemotePreference.CACHE(key = "get_group_refresh_$groupId", timeMillis = 8000)
        )
    }
}