package app.paralino.android.data.repos.personal_code

import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.repo.personal_code.IPersonalCodelLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.android.data.mappers.PersonalCodeMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonalCodeLocalDataSource(
    private val dao: PersonalCodeDao,
    private val mapper: PersonalCodeMapperLocal,
) : IPersonalCodelLocalDataSource {

    override suspend fun replacePersonalCodes(data: List<PersonalCodeRaw>) {
        dao.replaceAll(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun addPersonalCode(data: PersonalCodeRaw) {
        dao.createOrUpdate(mapper.remote2Local(data))
    }

    override fun getPersonalCodes(): Flow<List<UserPersonalCode>> {
        return dao.getAll()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Core(it) } }
    }

    override suspend fun deletePersonalCode(id: String) {
        dao.delete(id)
    }

    override suspend fun clear() {
        dao.clear()
    }
}