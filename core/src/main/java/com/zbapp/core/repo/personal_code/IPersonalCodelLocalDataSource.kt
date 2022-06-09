package com.zbapp.core.repo.personal_code

import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.raw.PersonalCodeRaw
import kotlinx.coroutines.flow.Flow

interface IPersonalCodelLocalDataSource {
    suspend fun replacePersonalCodes(data: List<PersonalCodeRaw>)
    suspend fun addPersonalCode(data: PersonalCodeRaw)
    fun getPersonalCodes(): Flow<List<UserPersonalCode>>
    suspend fun deletePersonalCode(id: String)
    suspend fun clear()
}