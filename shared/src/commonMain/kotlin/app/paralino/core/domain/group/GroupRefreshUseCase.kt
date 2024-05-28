package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.GroupWithMembersAndPlacesRaw
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException

class GroupRefreshUseCase constructor(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placesLocal: PlacesLocalDataSource,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupMapperCore: GroupMapperCore,
    private val groupCryptoKeyRotation: GroupCryptoKeyRotation,
    private val groupRemove: GroupRemoveUseCase,
) : LimitedExecutionRepository() {

    suspend fun refreshRemoteGroupWithMembersAndPlaces(groupId: String, cacheTimeMillis: Long): GroupWithMembersAndPlacesRaw? {
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(key = "group_refresh_$groupId", timeMillis = cacheTimeMillis),
            remote = {
                getRemoteGroupWithMembersAndPlaces(groupId)
                    .also {
                        logg.tag("tag_crypto_key_group").d("Finished processing group and keys, save group ${it.group.group.id}")
                        local.updateOrCreateGroup(it.group)
                        placesLocal.recreatePlacesForGroup(groupId = groupId, places = it.places)
                    }
            }
        )
    }

    private suspend fun getRemoteGroupWithMembersAndPlaces(groupId: String, retry: Int = 1): GroupWithMembersAndPlacesRaw {
        return try {
            triggerRemoteOnly {
                remote.getGroupWithMembers(groupId)
                    .also { cryptoKEKRepo.refreshKeysForGroup(groupId, it, force = false) }
                    .let { handleGroupKeysUpdate(it, retry) }
            }
        } catch (e: Exception) {
            if (e.asRemoteException() is NotFoundException) {
                groupRemove.clearGroupLocally(groupId)
            }
            throw e
        }
    }

    suspend fun handleGroupKeysUpdate(group: GroupWithMembersAndPlacesRaw, retry: Int = 1): GroupWithMembersAndPlacesRaw {

        if(retry > 2) {
            logg.tag("tag_crypto_key_group").w("Too many retries ($retry) in group refresh / key rotation for ${group.group.group.id}")
            return group
        }

        logg.tag("tag_crypto_key_group").d("got new group from remote, handle key rotation | retry #$retry")
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group.group)

        if(!remoteGroup.isGroupVerified)
            logg.tag("tag_crypto_key_group").e("Current member is unverified in group ${group.group.group.id}")

        if (groupCryptoKeyRotation.rotateGroupAndMemberKeys(remoteGroup, force = false)) {
            logg.tag("tag_crypto_key_group").d("Key rotation finished, refresh group and keys")
            return getRemoteGroupWithMembersAndPlaces(remoteGroup.id, retry + 1)
        } else {
            logg.tag("tag_crypto_key_group").d("No key rotation, use current group")
            return group
        }
    }
}