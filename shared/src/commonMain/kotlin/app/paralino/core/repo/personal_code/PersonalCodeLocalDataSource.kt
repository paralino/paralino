package app.paralino.core.repo.personal_code

import app.paralino.core.db.AppDatabaseHelper
import app.paralino.core.logg
import app.paralino.core.models.mappers.PersonalCodeMapperLocal
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonalCodeLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: PersonalCodeMapperLocal,
) {

    suspend fun replacePersonalCodes(data: List<PersonalCodeRaw>) {
        database.personalCodeDao.recreateCodes(data.map { mapper.remote2Local(it) })
    }

    fun getPersonalCodes(): Flow<List<UserPersonalCode>> {
        return database.personalCodeDao.getAllCodes()
            .distinctFlowOfList()
            .map {
                it.mapNotNull {
                    try {
                        mapper.local2Core(it)
                    } catch (e: Exception) {
                        logg.w("account_state", "Error mapping personal code: ${e.message}", e)
                        null
                    }
                }
            }
    }


    suspend fun clear() {
        database.personalCodeDao.clearAll()
    }
}