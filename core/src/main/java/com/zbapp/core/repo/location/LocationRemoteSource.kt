package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberGroupId
import com.zbapp.core.models.raw.LocationHistoryRaw
import com.zbapp.core.models.raw.LocationNodePayload
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.models.raw.UpdateLocationFunctionData
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.exceptions.BaseRemoteException
import com.zbapp.core.utils.exceptions.MissingGroupKeyException
import com.zbapp.core.utils.exceptions.MissingMemberKeyException
import com.zbapp.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class LocationRemoteSource constructor(
    private val api: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository
) {

    suspend fun getCurrentLocationsForGroup(groupId: String): List<MemberDocumentRaw> {
        return blocking {
            api.getDocuments(remoteConfig.resourceIds.collectionGroupMembersID).map {
                MemberDocumentRaw.from(it)
            }
        }
    }

    suspend fun getLocationsHistoryForUser(groupId: String, userId: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun getMyLocationsHistory(date: String): LocationHistoryRaw {
        TODO("Not yet implemented")
    }

    suspend fun saveCurrentLocation(
        ids: List<MemberGroupId>
    ) {
        blocking {
            val nodes = ids.map {

                val memberKey = cryptoKEKRepo.getMemberKey(it.groupId, config.userId)
                    ?: throw MissingMemberKeyException(it.groupId, config.userId)

                val encryptedData = it.data?.let {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(it),
                        key = memberKey.data.content
                    )
                }

                LocationNodePayload(
                    memberId = it.memberId,
                    groupId = it.groupId,
                    mutableData = encryptedData,
                    memberKeyVersion = memberKey.version
                )
            }

            val payload = UpdateLocationFunctionData(
                data = nodes
            )

            val response = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionLocationUpdateID,
                data = BaseRemoteModule.formatter.encodeToString(payload)
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = null,
                    message = "Unable to update location",
                    cause = Exception(response.stderr)
                )
            }
        }
    }

}