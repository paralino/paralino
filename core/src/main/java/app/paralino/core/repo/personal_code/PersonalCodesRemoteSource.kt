package app.paralino.core.repo.personal_code

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.days

class PersonalCodesRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val personalCodeMapper: PersonalCodeMapperCore,
) {

    suspend fun createNewPersonalCode(
        codeData: UserPersonalCodeData,
    ): PersonalCodeRaw {

        val userCodeData = UserPersonalCode(
            id = "",
            validityUnix = nowUTC().plus(7.days).epochSeconds,
            data = codeData
        )

        val codePayload = personalCodeMapper.core2Remote(userCodeData)

        val doc = docApi.createOrUpdateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionPersonalCodeID,
            docId = null,
            data = codePayload.toPostJson()
        )

        return PersonalCodeRaw.from(doc)
    }

    suspend fun getMyPersonalCodes(): List<PersonalCodeRaw> {
        return docApi.getDocuments(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionPersonalCodeID)
            .map { PersonalCodeRaw.from(it) }
    }

    suspend fun getUserInfoFromCode(code: String): UserPersonalCodeData {
        val codeKeys = personalCodeMapper.deriveCodeToKeys(code)
        return functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeReadID,
            data = codeKeys.loginPassword
        )
            .takeIf { it.isSuccess }
            ?.also {
                if(it.response.isBlank())
                    throw NotFoundException("Code not found")
            }
            ?.let {
                personalCodeMapper.remote2CoreRead(code, it.response)
            }!!
    }

    suspend fun revokePersonalCode(id: String) {
        docApi.deleteDocument(databaseType = BackendDatabaseType.DATA, remoteConfig.resourceIds.collectionPersonalCodeID, id)
    }

}