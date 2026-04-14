package app.paralino.core.domain.user

import app.paralino.core.models.raw.PersonalDeviceDeletePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository

class DeletePersonalDeviceUseCase(
    private val functionsApi: IFunctionsApi,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
): BaseRepository() {
    suspend fun deletePersonalDevice(deviceId: String) {
        triggerRemoteOnly {
            val payload = PersonalDeviceDeletePayload(
                deviceId = deviceId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.USER_DEVICE_DELETE,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            } else {
                personalSpaceRefresh.refreshPersonalSpace(cacheTimeMillis = 0)
            }
        }
    }
}