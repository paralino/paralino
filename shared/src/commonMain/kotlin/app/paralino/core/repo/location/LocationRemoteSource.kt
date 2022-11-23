package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.LocationNodePayload
import app.paralino.core.models.raw.LocationsPayload
import app.paralino.core.models.raw.UpdateLocationFunctionData
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class LocationRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
) {

    suspend fun saveCurrentLocation(
        ids: List<MemberGroupId>,
    ) {
        blocking {
            val nodes = ids.groupBy { it.groupId }.map { entry ->

                val memberKey = cryptoKEKRepo.getMemberKey(entry.key, config.userId)
                    ?: throw MissingMemberKeyException(entry.key, config.userId)

                val groupKey = cryptoKEKRepo.getGroupKey(entry.key)
                    ?: throw MissingGroupKeyException(entry.key)

                val locations = entry.value.map {

                    val encryptedData = it.data?.let {
                        cryptoCore.encryptDataWithKey(
                            data = BaseRemoteModule.formatter.encodeToString(it),
                            key = memberKey.data.content
                        )
                    }

                    LocationsPayload(
                        locationId = it.locationId,
                        deviceData = null,
                        locationData = encryptedData
                    )
                }

                LocationNodePayload(
                    groupId = entry.key,
                    memberData = null,
                    groupKeyVersion = groupKey.version,
                    memberKeyVersion = memberKey.version,
                    locations = locations
                )
            }

            val payload = UpdateLocationFunctionData(
                data = nodes
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionLocationUpdateID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = null,
                    message = "Unable to update location",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

}