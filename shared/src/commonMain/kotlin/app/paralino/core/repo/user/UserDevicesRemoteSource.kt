package app.paralino.core.repo.user

import app.paralino.core.config
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserDeviceFunctionData
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.extensions.blocking

class UserDevicesRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val userMemberIdUseCase: UserMemberIdUseCase
) {

    suspend fun getDevicesWithData(): List<UserDeviceRaw> {
        return documentApi.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionUserDevicesID
        ).map { UserDeviceRaw.from(it) }
    }

    suspend fun getCurrentDevicesWithData(): UserDeviceRaw {
        return documentApi.getDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionUserDevicesID,
            documentId = config.sessionId
        ).let { UserDeviceRaw.from(it) }
    }

    suspend fun updateDeviceData(pushToken: String) {
        blocking {
            val payload = UpdateUserDeviceFunctionData(
                sessionId = config.sessionId,
                deviceId = config.deviceId,
                pushToken = pushToken,
                deviceType = userMemberIdUseCase.getCurrentDeviceType().key
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.DEVICE_UPDATE,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

}