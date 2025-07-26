package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.IPersonalDeviceCryptoKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserDeviceMapper
import app.paralino.core.models.raw.UserDevicesWithLocationsAndPlacesRaw
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.user.UserDevicesRemoteSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException

class PersonalSpaceRefreshUseCase(
    private val userRepository: UserRepository,
    private val remote: UserDevicesRemoteSource,
    private val local: UserPersonalSpaceLocalDataSource,
    private val mapper: UserDeviceMapper,
    private val deviceKeyRotation: IPersonalDeviceCryptoKeyRotation,
    private val appPrefs: AppPrefs,
): LimitedExecutionRepository() {

    suspend fun refreshPersonalSpace(cacheTimeMillis: Long): UserDevicesWithLocationsAndPlacesRaw? {
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(key = "personal_space_refresh", timeMillis = cacheTimeMillis),
            remote = {
                getPersonalSpaceData()
                    .also { local.recreateDevicesWithPlaces(it) }
            }
        )
    }

    private suspend fun getPersonalSpaceData(retry: Int = 1): UserDevicesWithLocationsAndPlacesRaw {
        return try {
            triggerRemoteOnly {
                remote.getAllPersonalSpaceData()
                    .also {
                        // TODO refresh missing user keys, could be rotated
                    }
                    .let { handleGroupKeysUpdate(it, retry) }
            }
        } catch (e: Exception) {
            if (e.asRemoteException() is NotFoundException) {
                // ensure user is available and session is valid
                // if session is expired, this will throw and session recovery will kick in
                userRepository.refreshUserIfAvailable()
            }
            throw e
        }
    }

    suspend fun handleGroupKeysUpdate(space: UserDevicesWithLocationsAndPlacesRaw, retry: Int = 1): UserDevicesWithLocationsAndPlacesRaw {

        if(retry > 2) {
            logg.tag("tag_crypto_key_personal").w("Too many retries ($retry) in personal space refresh / key rotation")
            return space
        }

        logg.tag("tag_crypto_key_personal").d("Got new personal space data from remote, handle key rotation | retry #$retry")

        val device = space.devices.firstOrNull {
            it.device.deviceId == appPrefs.deviceId
        }?.let {
            mapper.local2Core(
                entity = mapper.remote2Local(deviceRaw = it)
            )
        }

        if (deviceKeyRotation.rotateDeviceKeys(device = device, force = false)) {
            logg.tag("tag_crypto_key_group").d("Key rotation finished, refresh group and keys")
            return getPersonalSpaceData(retry + 1)
        } else {
            logg.tag("tag_crypto_key_group").d("No key rotation, use current group")
            return space
        }
    }
}