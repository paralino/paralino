package com.zbapp.core.repo.personal_code

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.user.UserPersonalCodeData
import com.zbapp.core.models.mappers.PersonalCodeMapperCore
import com.zbapp.core.models.raw.PersonalCodeRaw
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.extensions.nowUTC
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.days

class PersonalCodesRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val personalCodeMapper: PersonalCodeMapperCore
) {

    suspend fun createNewPersonalCode(
        codeData: UserPersonalCodeData
    ): PersonalCodeRaw {

        val userCodeData = UserPersonalCode(
            id = "",
            validityUnix = nowUTC().plus(7.days).epochSeconds,
            data = codeData
        )

        val codePayload = personalCodeMapper.core2Remote(userCodeData)

        val doc = docApi.createOrUpdateDocument(
            collectionId = remoteConfig.resourceIds.collectionPersonalCodeID,
            docId = null,
            data = codePayload.toPostJson()
        )

        return PersonalCodeRaw.from(doc)
    }

    suspend fun getMyPersonalCodes(): List<PersonalCodeRaw> {
        return docApi.getDocuments(remoteConfig.resourceIds.collectionPersonalCodeID)
            .map { PersonalCodeRaw.from(it) }
    }

    suspend fun getUserInfoFromCode(code: String): UserPersonalCodeData {
        val codeKeys = personalCodeMapper.deriveCodeToKeys(code)
        return functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeReadID,
            data = codeKeys.loginPassword
        )
            .takeIf { it.isSuccess }
            ?.let {
                personalCodeMapper.remote2CoreRead(code, it.response)
            }!!
    }

    suspend fun revokePersonalCode(id: String) {
        docApi.deleteDocument(remoteConfig.resourceIds.collectionPersonalCodeID, id)
    }

}