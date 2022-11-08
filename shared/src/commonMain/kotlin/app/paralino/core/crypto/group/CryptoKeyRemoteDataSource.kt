package app.paralino.core.crypto.group

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.DecryptedGroupKeyData
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.raw.*
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class CryptoKeyRemoteDataSource(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore
) {

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupKeysID
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getGroupKeyForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupKeysID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersKeysID
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = remoteConfig.resourceIds.collectionGroupMembersKeysID,
                keyValues = listOf("groupId" to groupId)
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun rotateMemberKeysForGroup(
        group: UserGroupWithMembers,
        groupKey: DecryptedGroupKeyData,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {

            val newSenderKey = cryptoCore.getNewSymmetricKey().content

            // update data for every user device
            // this way any device from user that gets to update the keys will update data for all other devices
            // no need for other devices to do the same since encryption key is unique per user
            val memberData = group.members.filter { it.userId == config.userId }.map {
                // TODO review:
                // if group key is somehow removed, this crashes because we can not decrypt the data
                // what is happening now is that remote data is re-encrypted
                // but since this data is user own data, we can replace it if remote is missing or wrong
                GroupMemberDataPayload(
                    memberId = it.id,
                    groupKeyVersion = groupKey.version,
                    memberData = cryptoCore.encryptDataWithKey(
                        BaseRemoteModule.formatter.encodeToString(it.data.data!!),
                        groupKey.data.content
                    ),
                    mutableMemberData = it.mutableData.data?.let {
                        cryptoCore.encryptDataWithKey(
                            BaseRemoteModule.formatter.encodeToString(it),
                            newSenderKey
                        )
                    } ?: ""
                )
            }

            val keys = group.members.map {
                GroupMemberKeyPayload(
                    userId = it.userId,
                    encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                        data = newSenderKey,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = it.publicKey
                    )
                )

            }

            val payload = UpdateGroupMemberKeysPayload(
                groupId = group.id,
                userPublicKey = myKey.publicKey,
                deleteOldKeys = true,
                encryptedSenderKeys = keys,
                memberData = memberData
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupMemberKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update group member keys",
                    Exception(response.stderr)
                )
            }
        }
    }

    suspend fun rotateGroupKeys(
        group: UserGroupWithMembers,
        places: List<GroupPlace>,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {

            val newGroupKey = cryptoCore.getNewSymmetricKey().content

            val groupData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(group.data.data!!),
                key = newGroupKey
            )

            val placesData = places.mapNotNull { place ->
                place.data.data?.let {
                    GroupPlaceDataUpdatePayload(
                        placeId = place.id,
                        placeData = cryptoCore.encryptDataWithKey(
                            data = BaseRemoteModule.formatter.encodeToString(place.data.data),
                            key = newGroupKey
                        )
                    )
                }
            }

            val usedKeys = group.members.map { it.groupKeyVersion } + places.map { it.keyVersion } + group.keyVersion

            val keys = group.members.map {
                GroupKeysPayload(
                    userId = it.userId,
                    encryptedGroupKey = cryptoCore.encryptDataWithKeyPair(
                        data = newGroupKey,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = it.publicKey
                    )
                )
            }

            val payload = UpdateGroupKeysPayload(
                groupId = group.id,
                userPublicKey = myKey.publicKey,
                keyVersionsToKeep = usedKeys.distinct(),
                encryptedGroupKeys = keys,
                groupData = groupData,
                places = placesData
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update group keys",
                    Exception(response.stderr)
                )
            }
        }
    }

    suspend fun createMemberKeysForSpecificMembers(
        groupId: String,
        members: List<MemberModel>,
        myKey: AsymmetricCryptoKeyData,
        currentSenderKey: DecryptedGroupKeyData?
    ) {
        blocking {

            // if no key is passed, create new one
            val newSenderKey = currentSenderKey?.data?.content ?: cryptoCore.getNewSymmetricKey().content

            val keys = members.map {
                GroupMemberKeyPayload(
                    userId = it.userId,
                    encryptedSenderKey = cryptoCore.encryptDataWithKeyPair(
                        data = newSenderKey,
                        // sign with owner private key
                        myPrivateKey = myKey.privateKey,
                        // encrypt with user public key
                        publicKey = it.publicKey
                    ),
                    keyVersion = currentSenderKey?.version
                )
            }

            val payload = UpdateGroupMemberKeysPayload(
                groupId = groupId,
                userPublicKey = myKey.publicKey,
                deleteOldKeys = false,
                encryptedSenderKeys = keys,
                memberData = emptyList()
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUpdateGroupMemberKeysID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update missing group member keys",
                    Exception(response.stderr)
                )
            }
        }
    }

    private fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

}