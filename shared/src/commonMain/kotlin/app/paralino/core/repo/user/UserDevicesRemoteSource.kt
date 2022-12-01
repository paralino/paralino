package app.paralino.core.repo.user

import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserDeviceFunctionData
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

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
            documentId = userMemberIdUseCase.getCurrentSessionId()
        ).let { UserDeviceRaw.from(it) }
    }

    suspend fun updateDeviceData(pushToken: String) {
        blocking {
            val payload = UpdateUserDeviceFunctionData(
                sessionId = userMemberIdUseCase.getCurrentSessionId(),
                pushToken = pushToken,
                deviceType = userMemberIdUseCase.getCurrentDeviceType().key
            )

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionUserDeviceUpdate,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to update device data",
                    Exception(data.stderr)
                )
            }
        }
    }

}