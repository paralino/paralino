package app.paralino.core.repo.user

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.user.UserDeviceEntityDeviceUpdate
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.mappers.UserDeviceMapper
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.models.raw.UserDeviceWithLocationRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonalDevicesLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val deviceMapper: UserDeviceMapper,
) {

    suspend fun recreateDevices(devices: List<UserDeviceWithLocationRaw>) {
        database.userDeviceDao.recreateUserDevices(
            devices = devices.map { deviceMapper.remote2Local(it) }
        )
    }

    fun getAllUserDevices(): Flow<List<UserDeviceModel>> {
        return database.userDeviceDao.getAllUserDevices()
            .distinctFlowOfList()
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        deviceMapper.local2Core(entity)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    fun getAllUserDevicesRawPresentation(): Flow<List<Pair<UserDeviceWithLocationRaw, UserDeviceModel>>> {
        return database.userDeviceDao.getAllUserDevices()
            .distinctFlowOfList()
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        deviceMapper.local2Remote(entity) to deviceMapper.local2Core(entity)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun getUserDeviceByDeviceIdOnce(deviceId: String): UserDeviceModel? {
        return database.userDeviceDao.getUserDeviceByDeviceIdOnce(deviceId)?.let { entity ->
            try {
                deviceMapper.local2Core(entity)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getUserDeviceByIdOnce(id: String): UserDeviceModel? {
        return database.userDeviceDao.getUserDeviceOnce(id)?.let { entity ->
            try {
                deviceMapper.local2Core(entity)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun updateUserDeviceData(device: UserDeviceRaw) {
        database.userDeviceDao.updateUserDeviceData(
            dataUpdate = UserDeviceEntityDeviceUpdate(
                id = device.id,
                deviceId = device.deviceId,
                pushTargetId = device.pushTargetId,
                sessionId = device.sessionId,
                ownerUserId = device.ownerUserId,
                deviceData = device.deviceData,
                locationPushToken = device.locationPushToken,
                deviceType = device.deviceType,
                encryptedSenderKey = device.encryptedSenderKey,
                userKeyVersion = device.userKeyVersion,
                batteryAlertSettings = device.batteryAlertSettings,
                isUsingNotificationSockets = device.isUsingNotificationSockets
            )
        )
    }
} 