package app.paralino.core.domain.crypto

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.IUserLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class RawCryptoPresentation constructor(
    private val placeLocal: IPlacesLocalDataSource,
    private val groupLocal: IGroupLocalDataSource,
    private val memberLocal: ILocationLocalDataSource,
    private val userLocal: IUserLocalDataSource
) {

    fun getGroupRawPresentation(groupId: String): Flow<RawPresentationModel> {
        return groupLocal.getGroupRawPresentation(groupId)
            .map {
                RawPresentationModel(
                    title = it.second.data.data?.name ?: "",
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPresentation(groupId)
                )
            }
    }

    fun getGroupPlaceRawPresentation(placeId: String): Flow<RawPresentationModel> {
        return placeLocal.getPlaceRawPresentation(placeId)
            .map {
                RawPresentationModel(
                    title = it.second.data.data?.name ?: "",
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPlacePresentation(placeId)
                )
            }
    }

    fun getGroupMemberRawPresentation(memberId: String): Flow<RawPresentationModel> {
        return memberLocal.getMemberRawPresentation(memberId)
            .map {

                val rawLocationData = it.first.locations.map { mapOf("deviceData" to it.deviceData, "locationData" to it.locationData) }.let {
                    BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                }

                val rawData = "memberData: ${it.first.member.data}\n\nlocations: $rawLocationData"

                val decryptedMemberData = it.second.member.memberData.let {
                    when (it) {
                        is CryptoResource.Ready -> {
                            BaseRemoteModule.prettyPrintFormatter.encodeToString(it.data)
                        }
                        CryptoResource.NotInitialized -> "N/A"
                        else -> it.toString()
                    }
                }.let {
                    "memberData: $it"
                }
                val decryptedLocationData = it.second.locations.map {
                    mapOf(
                        "deviceData" to it.deviceData.let {
                            when (it) {
                                is CryptoResource.Ready -> {
                                    BaseRemoteModule.prettyPrintFormatter.encodeToString(it.data)
                                }
                                CryptoResource.NotInitialized -> "N/A"
                                else -> it.toString()
                            }
                        },
                        "locationData" to it.locationData.let {
                            when (it) {
                                is CryptoResource.Ready -> {
                                    BaseRemoteModule.prettyPrintFormatter.encodeToString(it.data)
                                }
                                CryptoResource.NotInitialized -> "N/A"
                                else -> it.toString()
                            }
                        }
                    )
                }.let {
                    BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                }.let {
                    "locations: $it"
                }
                val decryptedData = "${decryptedMemberData}\n\n$decryptedLocationData"

                RawPresentationModel(
                    title = it.second.member.memberData.data?.name ?: "",
                    rawData = rawData,
                    decryptedData = CryptoResource.Ready(decryptedData),
                    type = RawPresentationDataType.GroupMemberPresentation(memberId)
                )
            }
    }

    fun getUserRawPresentation(): Flow<RawPresentationModel> {
        return userLocal.getMeRawPresentation()
            .map {
                RawPresentationModel(
                    title = it?.second?.data?.userName ?: "",
                    rawData = it?.first?.data?.let {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    } ?: "",
                    decryptedData = it?.second?.data?.let {
                        CryptoResource.Ready(BaseRemoteModule.prettyPrintFormatter.encodeToString(it))
                    } ?: CryptoResource.WrongKeyOrData,
                    type = RawPresentationDataType.UserPresentation
                )
            }
    }

}

data class RawPresentationModel constructor(
    val title: String,
    val rawData: String,
    val decryptedData: CryptoResource<String>,
    val type: RawPresentationDataType
)