package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation.GroupDeviceSenderKeyData
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.raw.PersonalDeviceKeyUpdatePayload
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.nowUTC

abstract class IPersonalDeviceCryptoKeyRotation : LimitedExecution() {
    abstract suspend fun rotateDeviceKeys(
        device: UserDeviceModel?,
        force: Boolean = false
    ): Boolean
}

class NoOpPersonalDeviceKeyRotation: IPersonalDeviceCryptoKeyRotation() {
    override suspend fun rotateDeviceKeys(device: UserDeviceModel?, force: Boolean): Boolean {
        logg.tag("tag_crypto_key_personal_device_rotation")
            .w("Personal space NOOP operation")
        return false
    }
}

class PersonalDeviceCryptoKeyRotation(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val cryptoKEKRepository: CryptoKEKLocalRepository,
    private val validityVerification: GroupValidityVerification,
    private val deviceStatusProvider: MemberDeviceStatusProvider,
    private val locationSharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs
) : IPersonalDeviceCryptoKeyRotation() {
    private val executionKey = UserGroup.groupPersonalDevicesId

    override suspend fun rotateDeviceKeys(
        device: UserDeviceModel?,
        force: Boolean
    ): Boolean {

        if (device == null || device.deviceId != appPrefs.deviceId) {
            logg.tag("tag_crypto_key_personal_device_rotation").w("Device missing")
            return false
        }

        if (device.userKeyVersion == null) {
            logg.tag("tag_crypto_key_personal_device_rotation").w("Missing user key version")
            return false
        }

        if (device.location == null) {
            logg.tag("tag_crypto_key_personal_device_rotation")
                .v("Current device is not activated, skip key rotation")
            return false
        }

        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_personal_device_rotation")
                .v("Personal space device key rotation already in progress")
            return false
        }

        val deviceIntegrity =
            validityVerification.checkIntegrityForPersonalSpace(currentDevice = device)

        logg.tag("tag_crypto_key_personal_device_rotation")
            .v("Personal device integrity: $deviceIntegrity")

        if (deviceIntegrity == IntegrityResult.CORRUPTED) {
            logg.tag("tag_crypto_key_personal_device_rotation")
                .w("Personal device integrity corrupted, skipping key rotation")
            stopProcessing(executionKey)
            return false
        }

        val shouldRotate = force || deviceIntegrity.shouldChange

        if (!shouldRotate) {
            logg.tag("tag_crypto_key_personal_device_rotation")
                .d("Not rotating device keys for personal space")
            stopProcessing(executionKey)
            return false
        }

        logg.tag("tag_crypto_key_personal_device_rotation")
            .i("Personal device key is deprecated - rotating")

        try {
            rotateDeviceKeysImpl(device)
        } finally {
            stopProcessing(executionKey)
        }

        return true
    }

    private suspend fun rotateDeviceKeysImpl(device: UserDeviceModel) {

        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val newKey = cryptoCore.getNewSymmetricKeyForLocationData()

        val locationData = device.location?.locationData?.data?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = newKey
            )
        }

        val deviceData = device.location?.deviceData?.data?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKey.key
            )
        }

        val status = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.formatter.encodeToString(
                deviceStatusProvider.getStatus(groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
            ),
            key = newKey
        )

        val history = device.location?.locationHistory?.data?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.locationHistoryFormatter.encodeToString(it),
                key = newKey
            )
        }

        val payload = PersonalDeviceKeyUpdatePayload(
            deviceId = device.deviceId,
            userKeyVersion = userKey.version,
            encryptedSenderKey = cryptoCore.encryptDataWithKey(
                data = newKey,
                key = userKey.key
            ),
            deviceData = deviceData ?: "",
            locationData = locationData ?: "",
            status = status,
            locationHistory = history ?: ""
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_SPACE,
            data = payload
        )

        if (!response.isSuccess) {
            throw BaseRemoteException.fromRemote(
                code = response.statusCode,
                error = response.errorResponse
            )
        } else {
            val groupDeviceIntegrity = GroupIntegrity(
                timeUtc = nowUTC().toString(),
                signature = GroupIntegrity.createSignature(
                    hashProvider = cryptoCore.hashProvider,
                    serverIntegrity = device.sessionId ?: "",
                    memberVerifications = listOf(userKeyRepo.signatureKeyPair().publicKey),
                    encryptedMemberPublicKeys = listOf(userKeyRepo.signatureKeyPair().publicKey)
                )
            ).let {
                cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(it),
                    key = userKey.key
                )
            }

            cryptoKEKRepository.updateIntegrity(
                groupId = UserGroup.groupPersonalDevicesId,
                integrity = groupDeviceIntegrity,
                safetySignature = "",
                userKeyVersion = userKey.version
            )

            logg.tag("tag_crypto_key_personal_device_rotation")
                .i("Finished rotating personal device key")
        }
    }
}