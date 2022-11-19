package app.paralino.core.repo.user

import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import kotlinx.coroutines.flow.Flow

interface IUserLocalDataSource {

    fun getMe(): Flow<UserMe?>
    fun getMeRaw(): Flow<UserMeRaw?>
    fun getMeRawPresentation(): Flow<Pair<UserMeRaw, UserMe>?>

    suspend fun update(data: UserMeRaw): UserMe
    suspend fun clear()

}