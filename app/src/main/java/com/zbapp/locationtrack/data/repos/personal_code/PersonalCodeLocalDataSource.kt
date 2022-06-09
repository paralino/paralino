package com.zbapp.locationtrack.data.repos.personal_code

import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.raw.PersonalCodeRaw
import com.zbapp.core.repo.personal_code.IPersonalCodelLocalDataSource
import com.zbapp.core.utils.extensions.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.PersonalCodeMapperLocal
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