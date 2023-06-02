package app.paralino.core.domain.crypto

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GenericFunctionPayload
import app.paralino.core.models.raw.UpdateOwnMemberPublicKeyPayload
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.encodeToString

class MemberCryptoKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepository: CryptoKEKRepository,
    private val validityVerification: GroupValidityVerification,
) : LimitedExecution() {

    suspend fun rotateMemberKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
    ): Boolean {

        // if group is not verified (current member is not verified) skip key generation
        if (!remoteGroup.isGroupVerified) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .v("Group ${remoteGroup.id} not verified")
            return false
        }

        try {
            startProcessing(remoteGroup.id)
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .v("Group ${remoteGroup.id} member key rotation already in progress")
            return false
        }

        val integrity = validityVerification.checkIntegrityForMemberKey(remoteGroup)
        logg.tag("tag_crypto_key_group_member_rotation")
            .v("Group ${remoteGroup.id} integrity: $integrity")

        if (integrity == IntegrityResult.CORRUPTED) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .w("Group ${remoteGroup.id} integrity corrupted, skipping key rotation")
            stopProcessing(remoteGroup.id)
            return false
        }

        val shouldRotate = force || integrity == IntegrityResult.VALID_EXPIRED

        if (!shouldRotate) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .w("not rotating member keys for group: ${remoteGroup.id}")
            stopProcessing(remoteGroup.id)
            return false
        }

        logg.tag("tag_crypto_key_group_member_rotation")
            .w("group member key is deprecated - rotating: ${remoteGroup.id}")

        try {
            rotateMemberKeysForGroup(remoteGroup)
        } finally {
            stopProcessing(remoteGroup.id)
        }

        return true
    }

    private suspend fun rotateMemberKeysForGroup(
        group: UserGroupWithMembers
    ) {
        val myKey = userKeyRepo.latestKeyPair()
        val currentExchangeKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = null)
        val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)
        val groupKey =
            cryptoKEKRepository.getGroupKey(group.id, group.ownerUserId, keyVersion = null)

        if (currentExchangeKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, config.userId)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, config.userId)

        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(group.id)

        val newExchangeKeys = cryptoCore.getNewCryptoKeyPair()

        val member = group.members.filter { it.member.userId == config.userId }.firstOrNull()!!

        val groupIntegrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                memberVerifications = group.members.map { it.member.verification },
                encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMembershipPublicKeys(
                    groupId = group.id
                )
            )
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKeyRepo.personalKey()
            )
        }

        val memberKeyVersionsToKeep = cryptoKEKRepository.getAllActiveMemberKeyVersions(group.id)

        logg.tag("tag_crypto_key_group_member_rotation").v("Keep member keys: $memberKeyVersionsToKeep")

        val payloadData = UpdateOwnMemberPublicKeyPayload(
            groupId = group.id,
            integrity = groupIntegrity,
            memberData = cryptoCore.encryptDataWithKey(
                BaseRemoteModule.formatter.encodeToString(member.member.memberData.data),
                groupKey.key
            ),
            signedPublicKey = cryptoCore.signData(
                data = newExchangeKeys.publicKey,
                myPrivateKey = myKey.privateKey
            ),
            encryptedMemberPersonalKeys = cryptoCore.encryptModel(
                data = SimpleKeyPairData(
                    privateKey = newExchangeKeys.privateKey,
                    publicKey = newExchangeKeys.publicKey
                ),
                key = userKeyRepo.personalKey()
            ),
            memberKeyVersion = currentExchangeKeys.version.plus(1),
            groupKeyVersion = groupKey.version,
            memberKeyVersionsToKeep = memberKeyVersionsToKeep
        )

        val payload = GenericFunctionPayload(
            route = "update-member-key",
            payload = payloadData
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