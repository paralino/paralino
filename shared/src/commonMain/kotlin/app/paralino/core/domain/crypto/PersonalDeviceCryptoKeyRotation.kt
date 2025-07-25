package app.paralino.core.domain.crypto

import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation.GroupDeviceSenderKeyData
import app.paralino.core.models.location.LocationSharingContract

abstract class IPersonalDeviceCryptoKeyRotation: LimitedExecution() {
    abstract suspend fun rotateDeviceKeys(
        force: Boolean = false
    ): Boolean

    abstract suspend fun getNewSenderKeys(
        contract: LocationSharingContract?
    ): GroupDeviceSenderKeyData
}

class PersonalDeviceCryptoKeyRotation: IPersonalDeviceCryptoKeyRotation() {
    override suspend fun rotateDeviceKeys(force: Boolean): Boolean {
        return false
    }

    override suspend fun getNewSenderKeys(contract: LocationSharingContract?): GroupDeviceSenderKeyData {
        TODO("Not yet implemented")
    }
}