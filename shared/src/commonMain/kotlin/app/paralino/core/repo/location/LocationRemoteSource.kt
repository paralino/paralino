package app.paralino.core.repo.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.PersonalSpaceDeviceKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.raw.GroupLocationDeletePayload
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.models.raw.PersonalLocationDeletePayload
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking

class LocationRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val appBuildConfig: AppBuildConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val appPrefs: AppPrefs
) {

    suspend fun deleteLocationInGroup(memberId: String, locationId: String) {
        blocking {
            val payload = GroupLocationDeletePayload(
                memberId = memberId,
                locationId = locationId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.LOCATION_DELETE_GROUP,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }
        }
    }

    suspend fun clearLocation(
        groupId: GroupOrSpaceId,
        locationId: String
    ) {
        // passing fake data here so we hide the fact that users cleared their location
        blocking {
            val payload = LocationDataModel(
                lat = 0.0,
                lng = 0.0,
                accuracy = 10000000f,
                timeUTC = "2021-01-17T21:47:27+01:00",
                endTimeUTC = null,
                speed = null,
                altitude = null,
                isMasked = true
            )

            if (groupId.isPersonalSpace) {
                val deviceKey = userKeyRepo.getDeviceSenderKeyForLocationId(locationId = locationId)
                if (deviceKey !is PersonalSpaceDeviceKey.Valid) {
                    throw MissingMemberKeyException(groupId = groupId.id, memberId = appPrefs.userId)
                }

                val locationData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(payload),
                    key = deviceKey.key
                )

                documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionPersonalLocationsID,
                    docId = locationId,
                    data = mapOf(
                        "locationData" to locationData,
                        "locationHistory" to ""
                    )
                )
            } else {
                val memberKey = cryptoKEKRepo.getMemberSenderKey(
                    groupId = groupId.id,
                    memberUserId = appPrefs.userId,
                    locationId = locationId
                )

                if (memberKey !is GroupMemberSenderKey.Valid) {
                    throw MissingMemberKeyException(
                        groupId = groupId.id,
                        memberId = appPrefs.userId
                    )
                }

                val locationData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(payload),
                    key = memberKey.key
                )

                documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                    docId = locationId,
                    data = mapOf(
                        "locationData" to locationData,
                        "locationHistory" to ""
                    )
                )
            }
        }
    }

    suspend fun getMember(memberId: String): MemberWithLocationsRaw {
        return blocking {

            val member = documentApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appBuildConfig.resourceIds.collectionGroupMembersID,
                documentId = memberId
            ).let { MemberDocumentRaw.from(it) }

            val settings = if (member.ownerUserId == appPrefs.userId) {
                documentApi.getDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = appBuildConfig.resourceIds.collectionGroupSettingsID,
                    documentId = memberId
                ).let { GroupSettingsRaw.from(it) }
            } else {
                null
            }

            val locations = documentApi.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
                queries = listOf(BackendQuery.Equals(key = "groupId", value = member.groupId))
            ).map { MemberLocationRaw.from(it) }

            MemberWithLocationsRaw(
                member = member.copy(settingsRaw = settings),
                locations = locations.filter { location -> member.locationIds.contains(location.id) }
            )
        }
    }
}