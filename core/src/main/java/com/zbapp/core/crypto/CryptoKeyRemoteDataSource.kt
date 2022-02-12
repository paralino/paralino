package com.zbapp.core.crypto

import com.zbapp.core.config
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.*
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteException
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.blocking
import kotlinx.serialization.encodeToString

class CryptoKeyRemoteDataSource(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore
) {

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupKeysID)
                .map { it.parseGroupKey() }
        }
    }

    suspend fun getGroupKeyForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(
                collectionId = remoteConfig.resourceIds.collectionGroupKeysID,
                keyValue = arrayOf("groupId" to groupId)
            ).map { it.parseGroupKey() }
        }
    }

    suspend fun getAllGroupMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(remoteConfig.resourceIds.collectionGroupMembersKeysID)
                .map { it.parseGroupMemberKey() }
        }
    }

    suspend fun getAllGroupMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return blocking {
            api.getDocumentsFor(
                remoteConfig.resourceIds.collectionGroupMembersKeysID,
                "groupId" to groupId
            ).map { it.parseGroupMemberKey() }
        }
    }

    suspend fun rotateMemberKeysForGroup(
        group: UserGroup,
        groupKey: DecryptedGroupKeyData,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {

            val newSenderKey = cryptoCore.getNewSymmetricKey().content

            val memberData = group.members.filter { it.userId == config.userId }.map {
                GroupMemberDataPayload(
                    memberId = it.id,
                    groupKeyVersion = groupKey.version,
                    memberData = cryptoCore.encryptDataWithKey(
                        BaseRemoteModule.formatter.encodeToString(it.data),
                        groupKey.data.content
                    ),
                    mutableMemberData = it.mutableData?.let {
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

            val payload = UpdateGroupMemberKeysFunctionData(
                groupId = group.id,
                userPublicKey = myKey.publicKey,
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
        group: UserGroup,
        myKey: AsymmetricCryptoKeyData
    ) {
        blocking {

            val newGroupKey = cryptoCore.getNewSymmetricKey().content

            val groupData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(group.data),
                key = newGroupKey
            )

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

            val payload = UpdateGroupKeysFunctionData(
                groupId = group.id,
                userPublicKey = myKey.publicKey,
                encryptedGroupKeys = keys,
                groupData = groupData
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

    private fun Map<String, Any>.parseGroupKey(): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw.from(this)
    }

    private fun Map<String, Any>.parseGroupMemberKey(): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw.from(this)
    }

}