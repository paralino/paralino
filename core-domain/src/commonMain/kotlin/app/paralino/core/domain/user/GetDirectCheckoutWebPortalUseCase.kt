package app.paralino.core.domain.user

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

class GetDirectCheckoutWebPortalUseCase(
    private val functionsApi: IFunctionsApi
): LimitedExecutionRepository() {

    fun getWebPortalUrl(): Flow<WebCheckoutPortalResponse> {
        return getDataOnce {
            val response = functionsApi.runAndWait(
                function = ServerFunction.WEB_CHECKOUT_PORTAL_GET
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            response.response<WebCheckoutPortalResponse>().data
        }
            .startProcessing("web_portal_create")
            .flowOn(Dispatchers.IO)
    }

    @Serializable
    data class WebCheckoutPortalResponse(
        val url: String
    )
}