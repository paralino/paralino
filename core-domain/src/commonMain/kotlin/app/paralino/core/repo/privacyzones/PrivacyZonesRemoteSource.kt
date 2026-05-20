package app.paralino.core.repo.privacyzones

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.privacyzones.PrivacyZoneData
import app.paralino.core.models.raw.CreatePrivacyZonePayload
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.models.raw.UpdatePrivacyZonePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.extensions.blocking

class PrivacyZonesRemoteSource(
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
) {

    suspend fun createPrivacyZone(
        groupId: String,
        data: PrivacyZoneData,
    ): GroupPrivacyZoneRaw {
        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val encryptedData = cryptoCore.encryptModel(
            data = data,
            key = userKey.key,
        )

        val payload = CreatePrivacyZonePayload(
            groupId = groupId,
            userKeyVersion = userKey.version,
            data = encryptedData,
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.PRIVACY_ZONE_CREATE,
            data = payload,
        )

        if (response.isSuccess) {
            return response.responseDataMap!!.parsePrivacyZone()
        } else {
            throw response.remoteException
        }
    }

    suspend fun updatePrivacyZone(zone: GroupPrivacyZone): GroupPrivacyZoneRaw {
        return blocking {
            val zoneData = when (zone.data) {
                is CryptoResource.Ready -> zone.data.data
                else -> throw CryptoException("Privacy zone data is missing: ${zone.id}")
            }

            val userKey = userKeyRepo.personalKey(version = zone.userKeyVersion)
            val encryptedZoneData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(zoneData),
                key = userKey.key,
            )

            val payload = UpdatePrivacyZonePayload(
                privacyZoneId = zone.id,
                zoneData = encryptedZoneData,
                userKeyVersion = userKey.version,
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.PRIVACY_ZONE_UPDATE,
                data = payload,
            )

            if (response.isSuccess) {
                response.responseDataMap!!.parsePrivacyZone()
            } else {
                throw response.remoteException
            }
        }
    }

    suspend fun deletePrivacyZone(privacyZoneId: String) {
        blocking {
            val response = functionsApi.runAndWait(
                function = ServerFunction.PRIVACY_ZONE_DELETE,
                data = privacyZoneId,
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    private fun Map<String, Any>.parsePrivacyZone(): GroupPrivacyZoneRaw {
        return GroupPrivacyZoneRaw.from(this)
    }
}
