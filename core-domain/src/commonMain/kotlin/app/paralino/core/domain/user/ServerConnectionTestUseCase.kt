package app.paralino.core.domain.user

import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserRepository
import kotlin.time.Duration.Companion.seconds

class ServerConnectionTestUseCase(
    private val userConfigRepo: UserConfigRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val userRepository: UserRepository,
    private val functionsApi: IFunctionsApi
) {

    suspend fun runTest(): List<ServerConnectionTestStep> {
        return ServerConnectionTestStep.entries.filterNot { step ->
            runStep(step)
        }
    }

    private suspend fun runStep(step: ServerConnectionTestStep): Boolean {
        return try {
            when (step) {
                ServerConnectionTestStep.USER_CONFIG -> userConfigRepo.refreshMaybeAndGetConfig(cacheTime = 0.seconds) != null
                ServerConnectionTestStep.APP_CONFIG -> remoteConfigRepo.refreshConfig(cacheTimeMillis = 0) != null
                ServerConnectionTestStep.AUTH -> userRepository.refreshUserMaybe(timeDelayMillis = 0).data != null
                ServerConnectionTestStep.TEST_API -> testFunction(ServerFunction.TEST_API)
                ServerConnectionTestStep.TEST_PING -> testFunction(ServerFunction.TEST_PING)
                ServerConnectionTestStep.TEST_LOCATION_UPLOAD -> testFunction(ServerFunction.TEST_LOCATION_UPLOAD)
                ServerConnectionTestStep.TEST_HISTORY_UPLOAD -> testFunction(ServerFunction.TEST_HISTORY_UPLOAD)
            }
        } catch (e: Exception) {
            logg.w("Connection test failed for ${step.tag} - ${e.message}")
            false
        }.also { isSuccess ->
            logg.d("Connection test ${step.tag}: ${if (isSuccess) "OK" else "FAILED"}")
        }
    }

    private suspend fun testFunction(function: ServerFunction): Boolean {
        return functionsApi.runAndWait(function = function).isSuccess
    }
}

enum class ServerConnectionTestStep(val tag: String) {
    USER_CONFIG("user-config"),
    APP_CONFIG("app-config"),
    AUTH("auth"),
    TEST_API("test:api"),
    TEST_PING("test:ping"),
    TEST_LOCATION_UPLOAD("test:location"),
    TEST_HISTORY_UPLOAD("test:history")
}
