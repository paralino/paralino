package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.main.user.UserDeviceEntity
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberDeviceStatus
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.raw.UserDeviceWithLocationRaw
import app.paralino.core.utils.BaseRemoteModule

class UserDeviceMapper(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource
) {
    
    /**
     * Combines device raw and device location raw into entity
     */
    fun remote2Local(
        deviceRaw: UserDeviceWithLocationRaw
    ): UserDeviceEntity {
        return UserDeviceEntity(
            id = deviceRaw.device.id,
            pushTargetId = deviceRaw.device.pushTargetId,
            sessionId = deviceRaw.device.sessionId,
            deviceType = deviceRaw.device.deviceType,
            ownerUserId = deviceRaw.device.ownerUserId,
            deviceId = deviceRaw.device.deviceId,
            locationPushToken = deviceRaw.device.locationPushToken,
            encryptedSenderKey = deviceRaw.device.encryptedSenderKey,
            userKeyVersion = deviceRaw.device.userKeyVersion,
            locationId = deviceRaw.location?.id,
            deviceData = deviceRaw.location?.deviceData,
            locationData = deviceRaw.location?.locationData,
            status = deviceRaw.location?.status,
            locationHistory = deviceRaw.location?.locationHistory
        )
    }

    /**
     * Decrypts and maps entity to user device model
     */
    suspend fun local2Core(entity: UserDeviceEntity): UserDeviceModel {
        val deviceType = parseDeviceType(entity.deviceType)
        
        val location = if (!entity.hasValidLocation) {
            null
        } else {
            try {
                // First, decrypt the encrypted sender key using the user key
                val senderKey = userKeyRepo.getDeviceSenderKeyForDevice(device = entity).key

                // Now use the sender key to decrypt the location data
                val deviceData = decryptDeviceData(entity.deviceData, senderKey)
                val locationData = decryptLocationData(entity.locationData, senderKey)
                val status = decryptStatusData(entity.status, senderKey)
                val locationHistory = decryptLocationHistory(entity.locationHistory, senderKey)

                MemberLocation(
                    id = entity.id,
                    groupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                    memberId = "", // User devices don't have a member ID
                    userId = entity.ownerUserId,
                    deviceId = entity.deviceId,
                    groupKeyVersion = 0L, // Not applicable for user devices
                    deviceData = deviceData,
                    locationData = locationData,
                    locationHistory = locationHistory,
                    status = status
                )
            } catch (e: Exception) {
                null
            }
        }

        return UserDeviceModel(
            id = entity.id,
            sessionId = entity.sessionId,
            deviceType = deviceType,
            ownerUserId = entity.ownerUserId,
            deviceId = entity.deviceId,
            userKeyVersion = entity.userKeyVersion,
            location = location
        )
    }

    private fun parseDeviceType(deviceTypeString: String): DeviceType? {
        return when (deviceTypeString) {
            DeviceType.Android.key -> DeviceType.Android
            DeviceType.iOS.key -> DeviceType.iOS
            else -> null
        }
    }

    private suspend fun decryptDeviceData(
        encryptedData: String?,
        senderKey: String?
    ): CryptoResource<MemberDeviceData> {
        if (encryptedData.isNullOrBlank()) {
            return CryptoResource.NotInitialized
        }

        if (senderKey.isNullOrBlank()) {
            return CryptoResource.MissingKey
        }

        return try {
            val decryptedData = cryptoCore.decryptDataWithKey(
                data = encryptedData,
                key = senderKey
            )
            val deviceData = BaseRemoteModule.formatter.decodeFromString<MemberDeviceData>(decryptedData)
            CryptoResource.Ready(deviceData)
        } catch (e: Exception) {
            CryptoResource.WrongKeyOrData
        }
    }

    private suspend fun decryptLocationData(
        encryptedData: String?,
        senderKey: String?
    ): CryptoResource<LocationDataModel> {
        if (encryptedData.isNullOrBlank()) {
            return CryptoResource.NotInitialized
        }

        if (senderKey.isNullOrBlank()) {
            return CryptoResource.MissingKey
        }

        return try {
            val decryptedData = cryptoCore.decryptDataWithKey(
                data = encryptedData,
                key = senderKey
            )
            val locationData = BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(decryptedData)
            if (locationData.isMasked) {
                CryptoResource.NotInitialized
            }  else {
                CryptoResource.Ready(locationData)
            }
        } catch (e: Exception) {
            CryptoResource.WrongKeyOrData
        }
    }

    private suspend fun decryptStatusData(
        encryptedData: String?,
        senderKey: String?
    ): CryptoResource<MemberDeviceStatus> {
        if (encryptedData.isNullOrBlank()) {
            return CryptoResource.NotInitialized
        }

        if (senderKey.isNullOrBlank()) {
            return CryptoResource.MissingKey
        }

        return try {
            val decryptedData = cryptoCore.decryptDataWithKey(
                data = encryptedData,
                key = senderKey
            )
            val statusData = BaseRemoteModule.formatter.decodeFromString<MemberDeviceStatus>(decryptedData)
            CryptoResource.Ready(statusData)
        } catch (e: Exception) {
            CryptoResource.WrongKeyOrData
        }
    }

    private suspend fun decryptLocationHistory(
        encryptedData: String?,
        senderKey: String?
    ): CryptoResource<LocationHistoryModel> {
        if (encryptedData.isNullOrBlank()) {
            return CryptoResource.NotInitialized
        }

        if (senderKey.isNullOrBlank()) {
            return CryptoResource.MissingKey
        }

        return try {
            val decryptedData = cryptoCore.decryptDataWithKey(
                data = encryptedData,
                key = senderKey
            )
            val historyData = BaseRemoteModule.locationHistoryFormatter.decodeFromString<LocationHistoryModel>(decryptedData)
            CryptoResource.Ready(historyData)
        } catch (e: Exception) {
            CryptoResource.WrongKeyOrData
        }
    }
}