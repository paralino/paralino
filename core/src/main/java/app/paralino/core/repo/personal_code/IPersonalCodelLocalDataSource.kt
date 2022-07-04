package app.paralino.core.repo.personal_code

import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.raw.PersonalCodeRaw
import kotlinx.coroutines.flow.Flow

interface IPersonalCodelLocalDataSource {
    suspend fun replacePersonalCodes(data: List<PersonalCodeRaw>)
    suspend fun addPersonalCode(data: PersonalCodeRaw)
    fun getPersonalCodes(): Flow<List<UserPersonalCode>>
    suspend fun deletePersonalCode(id: String)
    suspend fun clear()
}