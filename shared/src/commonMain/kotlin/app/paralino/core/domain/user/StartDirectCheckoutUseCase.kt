package app.paralino.core.domain.user

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.base.PlanType
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

class StartDirectCheckoutUseCase(
    private val functionsApi: IFunctionsApi
): LimitedExecutionRepository() {

    fun createCheckout(preselectedPlan: PlanType?): Flow<CreateWebCheckoutSessionResponse> {
        return getDataOnce {
            val response = functionsApi.runAndWait(
                function = ServerFunction.CREATE_WEB_CHECKOUT_SESSION,
                data = CreateWebCheckoutSessionRequest(
                    planType = preselectedPlan?.key
                )
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            response.response<CreateWebCheckoutSessionResponse>().data
        }
            .startProcessing("direct_checkout")
            .flowOn(Dispatchers.IO)
    }

    @Serializable
    data class CreateWebCheckoutSessionRequest(
        val planType: String?
    )

    @Serializable
    data class CreateWebCheckoutSessionResponse(
        val url: String
    )
}