package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException

class GroupRefreshUseCase constructor(
    private val local: UserGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val placeRepo: PlacesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupMapperCore: GroupMapperCore,
    private val groupCryptoKeyRotation: GroupCryptoKeyRotation,
    private val groupRemove: GroupRemoveUseCase,
) : LimitedExecutionRepository() {

    suspend fun refreshRemoteGroupWithMembersAndPlaces(groupId: String): GroupWithMembersRaw {
        return getRemoteGroupWithMembers(groupId)
            .also {
                logg.tag("tag_crypto_key_group").w("Finished processing group and keys, save group ${it.group.id}")
                local.updateOrCreateGroup(it)
            }.also {
                placeRepo.refreshPlacesForGroup(groupId)
            }
    }

    private suspend fun getRemoteGroupWithMembers(groupId: String, retry: Int = 1): GroupWithMembersRaw {
        return try {
            triggerRemoteOnly {
                remote.getGroupWithMembers(groupId)
                    .also { cryptoKEKRepo.refreshKeysForGroup(groupId) }
                    .let { handleGroupKeysUpdate(it, retry) }
            }
        } catch (e: Exception) {
            if (e.asRemoteException() is NotFoundException) {
                groupRemove.clearGroupLocally(groupId)
            }
            throw e
        }
    }

    suspend fun handleGroupKeysUpdate(group: GroupWithMembersRaw, retry: Int = 1): GroupWithMembersRaw {

        if(retry > 2) {
            logg.tag("tag_crypto_key_group").w("Too many retries ($retry) in group refresh / key rotation for ${group.group.id}")
            return group
        }

        logg.tag("tag_crypto_key_group").d("got new group from remote, handle key rotation | retry #$retry")
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group)

        if(!remoteGroup.isGroupVerified)
            logg.tag("tag_crypto_key_group").e("Current member is unverified in group ${group.group.id}")

        if (groupCryptoKeyRotation.rotateGroupAndMemberKeys(remoteGroup, force = false)) {
            logg.tag("tag_crypto_key_group").d("Key rotation finished, refresh group and keys")
            return getRemoteGroupWithMembers(remoteGroup.id, retry + 1)
        } else {
            logg.tag("tag_crypto_key_group").d("No key rotation, use current group")
            return group
        }
    }
}