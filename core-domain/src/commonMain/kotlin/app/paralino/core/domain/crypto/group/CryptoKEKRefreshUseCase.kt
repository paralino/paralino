package app.paralino.core.domain.crypto.group

import app.paralino.core.logg
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw
import app.paralino.core.repo.base.BaseRepository
import kotlinx.coroutines.async

class CryptoKEKRefreshUseCase(
    private val remote: CryptoKeyRemoteDataSource,
    private val local: CryptoKeyLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKLocalRepository
): BaseRepository() {

    suspend fun refreshAllKeys(groups: List<GroupWithMembersAndPlacesRaw>, force: Boolean) {
        triggerRemoteOnly {

            if (!force && hasAllKeys(groups)) {
                logg.tag("tag_crypto_key").d("No need to refresh all keys for groups")
                return@triggerRemoteOnly
            }

            logg.tag("tag_crypto_key").d("Need to refresh all keys for all groups (${groups.size})")

            val groupKeysAsync = async { remote.getAllGroupKeys() }
            val memberKeysAsync = async { remote.getAllGroupMemberKeys() }
            val deviceKeysAsync = async { remote.getAllGroupDeviceKeys() }
            val preKeysAsync = async { remote.getAllPersonalGroupKeys() }

            val groupKeys = groupKeysAsync.await()
            val memberKeys = memberKeysAsync.await()
            val deviceKeys = deviceKeysAsync.await()
            val preKeys = preKeysAsync.await()

            local.recreateAllGroupKeys(groupKeys, memberKeys, deviceKeys, preKeys)

            logg.tag("tag_crypto_key")
                .w("updated group (${groupKeys.size}), member (${memberKeys.size}), device (${deviceKeys.size}), pre (${preKeys.size}) keys")
        }
    }

    suspend fun refreshGroupKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group").d("refreshing only group key")
            remote.getGroupKeyForGroup(groupId)
        }.also {
            logg.tag("tag_crypto_key_group").d("updated only group keys (${it.size})")
            local.recreateGroupKeyForGroup(groupId, it)
        }
    }

    suspend fun refreshMemberKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group_member").d("refreshing only group member key")
            val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member").d("refreshing group pre keys")
            val preKeys = remote.getAllPersonalGroupKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member")
                .d("updated member (${memberKeys.size}), pre (${preKeys.size}) keys")

            local.recreateMemberKeysForGroup(groupId, memberKeys, preKeys)
        }
    }

    suspend fun refreshDeviceKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group_member").w("refreshing only group device key")
            val deviceKeys = remote.getAllGroupDeviceKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member")
                .w("updated member device (${deviceKeys.size}) keys")

            local.recreateDeviceKeysForGroup(groupId, deviceKeys)
        }
    }

    suspend fun refreshKeysForGroup(
        groupId: String,
        group: GroupWithMembersAndPlacesRaw,
        force: Boolean
    ) {
        triggerRemoteOnly {

            if (!force && hasAllKeysForGroup(group)) {
                logg.tag("tag_crypto_key_group").d("No need to refresh keys for group $groupId")
                return@triggerRemoteOnly
            }

            logg.tag("tag_crypto_key_group").d("Need to refresh all keys for group $groupId")

            val groupKeysAsync = async { remote.getGroupKeyForGroup(groupId) }
            val memberKeysAsync = async { remote.getAllGroupMemberKeysForGroup(groupId) }
            val deviceKeysAsync = async { remote.getAllGroupDeviceKeysForGroup(groupId) }
            val preKeysAsync = async { remote.getAllPersonalGroupKeysForGroup(groupId) }

            val groupKeys = groupKeysAsync.await()
            val memberKeys = memberKeysAsync.await()
            val deviceKeys = deviceKeysAsync.await()
            val preKeys = preKeysAsync.await()

            logg.tag("tag_crypto_key_group")
                .d("Got new group (${groupKeys.size}), member (${memberKeys.size}), device (${deviceKeys.size}), pre (${preKeys.size}) for group $groupId")

            local.recreateGroupKeyForGroup(groupId, groupKeys, memberKeys, deviceKeys, preKeys)

            logg.tag("tag_crypto_key_group").d("updated group and member keys for group")
        }
    }

    private suspend fun hasAllKeys(groups: List<GroupWithMembersAndPlacesRaw>): Boolean {
        return groups.all { hasAllKeysForGroup(it) }
    }

    private suspend fun hasAllKeysForGroup(group: GroupWithMembersAndPlacesRaw): Boolean {
        buildList {
            add(group.group.group.groupKeyVersion)
            addAll(group.group.members.map { it.member.groupKeyVersion })
            addAll(group.group.members.flatMap { it.locations.map { it.groupKeyVersion } })
            addAll(group.places.map { it.place.groupKeyVersion })
        }
            .distinct()
            .all { cryptoKEKRepo.hasGroupKeyByVersion(groupId = group.group.group.id, groupKeyVersion = it) }
            .takeIf { it }
            ?: return false

        group.group.members.all { member ->
            member.locations.all { location ->
                cryptoKEKRepo.hasDeviceKeyByVersion(
                    locationId = location.id,
                    deviceKeyVersion = location.deviceKeyVersion
                )
            }
        }
            .takeIf { it }
            ?: return false

        group.group.members.all { member ->
            cryptoKEKRepo.hasMemberKeyByVersion(
                memberId = member.member.id,
                memberKeyVersion = member.member.memberKeyVersion
            )
        }
            .takeIf { it }
            ?: return false

        if (local.getMemberKeys().filter { it.groupId == group.group.group.id }.size != group.group.members.size) {
            return false
        }

        return true
    }
}