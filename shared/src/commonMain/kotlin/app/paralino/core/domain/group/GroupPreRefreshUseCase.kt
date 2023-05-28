package app.paralino.core.domain.group

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.repo.base.RemotePreference

class GroupPreRefreshUseCase constructor(
    private val groupCryptoKeyRotation: GroupCryptoKeyRotation,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val deviceKeyRotation: MemberDeviceKeyRotation,
    private val groupRemove: GroupRemoveUseCase,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupMemberAdd: GroupMemberAddUseCase,
    private val groupRefresh: GroupRefreshUseCase,
    private val refreshEverything: RefreshEverythingUseCase
) : LimitedExecutionRepository() {

    override fun isProcessing(key: String): Boolean {
        val selfProcessing = super.isProcessing(key)
        val fullProcessing = refreshEverything.isProcessing()
        val groupRemoveProcessing = groupRemove.isProcessing(key)
        val groupMemberRemoveProcessing = groupMemberRemove.isProcessing(key)
        val groupMemberAddProcessing = groupMemberAdd.isProcessing(key)
        val groupCryptoKeyRotationProcessing = groupCryptoKeyRotation.isProcessing(key)
        val memberCryptoKeyRotationProcessing = memberCryptoKeyRotation.isProcessing(key)
        val deviceKeyRotationProcessing = deviceKeyRotation.isProcessing(key)

        logg.tag("group_actions").v(
            "Group pre-refresh processing:" +
                    " self: $selfProcessing" +
                    " full: $fullProcessing" +
                    " groupRemove: $groupRemoveProcessing" +
                    " groupMemberRemove: $groupMemberRemoveProcessing" +
                    " groupAddRemove: $groupMemberAddProcessing" +
                    " groupKeyRotation: $groupCryptoKeyRotationProcessing" +
                    " memberKeyRotation: $memberCryptoKeyRotationProcessing" +
                    " deviceKeyRotation: $deviceKeyRotationProcessing"
        )

        return selfProcessing || fullProcessing
                || groupRemoveProcessing || groupMemberRemoveProcessing || groupMemberAddProcessing
                || groupCryptoKeyRotationProcessing || memberCryptoKeyRotationProcessing || deviceKeyRotationProcessing
    }

    suspend fun refreshGroupWithMembers(groupId: String, cacheTimeMillis: Long = 4_000) {

        try {
            startProcessing(groupId)
        } catch (e: Exception) {
            logg.tag("group_actions").v("Processing, can't refresh group $groupId")
            return
        }

        logg.tag("group_actions").v("Not processing, can refresh group $groupId")

        val key = "group_refresh_$groupId"
        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(key = key, timeMillis = cacheTimeMillis),
                remote = { groupRefresh.refreshRemoteGroupWithMembersAndPlaces(groupId) }
            )
        } catch (e: Exception) {
            logg.tag("group_actions")
                .w("Stop processing group refresh for group $groupId - ${e.message}")
        } finally {
            stopProcessing(groupId)
        }
    }
}