package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.UpdateOwnMemberPublicKeyPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.encodeToString

class MemberCryptoKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoKEKRepository: CryptoKEKLocalRepository,
    private val validityVerification: GroupValidityVerification,
    private val appPrefs: AppPrefs
) : LimitedExecution() {

    suspend fun rotateMemberKeys(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
    ): Boolean {

        // if group is not verified (current member is not verified) skip key generation
        if (!remoteGroup.isGroupVerified) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .w("Group ${remoteGroup.id} not verified")
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

        val shouldRotate = force || integrity.shouldChange

        if (!shouldRotate) {
            logg.tag("tag_crypto_key_group_member_rotation")
                .d("not rotating member keys for group: ${remoteGroup.id}")
            stopProcessing(remoteGroup.id)
            return false
        }

        logg.tag("tag_crypto_key_group_member_rotation")
            .i("group member key is deprecated - rotating: ${remoteGroup.id}")

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
        val myKey = userKeyRepo.signatureKeyPair()
        val myMembershipKeys = cryptoKEKRepository.getMyPreKeyForGroup(group.id, version = 0)
        val groupKey = cryptoKEKRepository.getGroupKey(group.id, group.ownerUserId, keyVersion = null)

        if (myMembershipKeys !is UserGroupPersonalKeyPair.Valid)
            throw MissingMemberKeyException(group.id, appPrefs.userId)

        if (groupKey !is GroupMasterKey.Valid)
            throw MissingGroupKeyException(group.id)

        val newExchangeKeys = cryptoCore.getNewCryptoKeyPair()

        val member = group.members.filter { it.member.userId == appPrefs.userId }.firstOrNull()!!

        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val groupIntegrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                serverIntegrity = group.group.serverIntegrity,
                memberVerifications = group.members.map { it.member.verification },
                encryptedMemberPublicKeys = cryptoKEKRepository.getAllEncryptedMembershipPublicKeys(
                    groupId = group.id
                )
            )
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKey.key
            )
        }

        val memberKeyVersionsToKeep = cryptoKEKRepository.getAllActiveMemberKeyVersions(group.id)

        logg.tag("tag_crypto_key_group_member_rotation").v("Keep member keys: $memberKeyVersionsToKeep")

        val payloadData = UpdateOwnMemberPublicKeyPayload(
            groupId = group.id,
            integrity = groupIntegrity,
            userKeyVersion = userKey.version,
            memberData = cryptoCore.encryptDataWithKey(
                BaseRemoteModule.formatter.encodeToString(member.member.memberData.data),
                groupKey.key
            ),
            paralinoId = member.member.paralinoId.data?.let {
                cryptoCore.encryptDataWithKey(
                    BaseRemoteModule.formatter.encodeToString(it),
                    groupKey.key
                )
            },
            signedPublicKey = cryptoCore.signData(
                data = newExchangeKeys.publicKey,
                myPrivateKey = myKey.privateKey
            ),
            encryptedMemberPersonalKeys = cryptoCore.encryptModel(
                data = SimpleKeyPairData(
                    privateKey = newExchangeKeys.privateKey,
                    publicKey = newExchangeKeys.publicKey
                ),
                key = userKey.key
            ),
            groupKeyVersion = groupKey.version,
            memberKeyVersionsToKeep = memberKeyVersionsToKeep
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_MEMBERS,
            data = payloadData
        )

        if (!response.isSuccess) {
            throw response.remoteException
        } else {
            logg.tag("tag_crypto_key_group_member_rotation")
                .i("Finished rotating group member key for: ${group.id}")
        }
    }
}