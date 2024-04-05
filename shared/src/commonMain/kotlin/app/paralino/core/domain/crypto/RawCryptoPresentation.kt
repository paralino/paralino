package app.paralino.core.domain.crypto

import app.paralino.core.data.db.AppConfig
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.user.UserLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class RawCryptoPresentation constructor(
    private val placeLocal: PlacesLocalDataSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val memberLocal: LocationLocalDataSource,
    private val userLocal: UserLocalDataSource,
    private val config: AppConfig
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getGroupRawPresentation(groupId: String): Flow<RawPresentationModel> {
        return groupLocal.getGroupRawPresentation(groupId)
            .flatMapLatest {
                val groupModel = RawPresentationModelSection(
                    title = it.second.name,
                    subtitle = null,
                    contentIdInfo = buildList {
                        // only allow group owner to see group id
                        if (it.first.ownerUserId == config.userId) {
                            add("GroupID: **$groupId**")
                        }
                    },
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPresentation(groupId)
                )
                val myMemberId =
                    memberLocal.getUserMemberId(groupId = groupId, userId = config.userId) ?: ""
                getGroupMemberRawPresentation(memberId = myMemberId)
                    .map {
                        RawPresentationModel(
                            buildList {
                                add(groupModel)
                                addAll(it.sections)
                            }
                        )
                    }
            }
    }

    fun getGroupPlaceRawPresentation(placeId: String): Flow<RawPresentationModel> {
        return placeLocal.getPlaceRawPresentation(placeId)
            .map {
                RawPresentationModelSection(
                    title = it.second.nameOrNull ?: "---",
                    subtitle = null,
                    contentIdInfo = buildList {
                        add("GroupID: **${it.first.groupId}**")
                        add("PlaceID: **$placeId**")
                    },
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPlacePresentation(placeId)
                )
            }
            .map { RawPresentationModel(listOf(it)) }
    }

    fun getGroupMemberRawPresentation(memberId: String): Flow<RawPresentationModel> {
        return memberLocal.getMemberRawPresentation(memberId)
            .map { rawPair ->

                fun locationDataPrint(
                    deviceData: String?,
                    locationData: String?,
                    status: String?
                ): String {
                    return buildString {
                        append("\"deviceData\": ")
                        append(deviceData)
                        append("\n\n")
                        append("\"locationData\": ")
                        append(locationData)
                        append("\n\n")
                        append("\"status\": ")
                        append(status)
                    }
                }

                val devicesSections = rawPair.first.locations.map { location ->
                    val decryptedLocationData = rawPair.second.locations
                        .firstOrNull { it.id == location.id }
                        ?.let {
                            CryptoResource.Ready(
                                locationDataPrint(
                                    deviceData = it.deviceData.mapSuccess {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                                    }.data,
                                    locationData = it.locationData.mapSuccess {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                                    }.data,
                                    status = it.status.mapSuccess {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                                    }.data
                                )
                            )
                        } ?: CryptoResource.NotInitialized

                    RawPresentationModelSection(
                        title = null,
                        subtitle = null,
                        contentIdInfo = buildList {
                            add("DeviceID: **${location.deviceId}**")
                        },
                        rawData = locationDataPrint(
                            deviceData = location.deviceData,
                            locationData = location.locationData,
                            status = location.status
                        ),
                        decryptedData = decryptedLocationData,
                        type = RawPresentationDataType.GroupMemberPresentation(memberId)
                    )
                }

                buildList {
                    add(
                        RawPresentationModelSection(
                            title = rawPair.second.member.name,
                            subtitle = null,
                            contentIdInfo = buildList {
                                add("UserID: **${config.userId}**")
                                add("MemberID: **${rawPair.first.member.id}**")
                                add("Verification: **${rawPair.second.member.verificationResult}**")
                            },
                            rawData = rawPair.first.member.data,
                            decryptedData = rawPair.second.member.memberData.mapSuccess {
                                BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                            },
                            type = RawPresentationDataType.GroupMemberPresentation(memberId)
                        )
                    )
                    addAll(devicesSections)
                }
            }
            .map { RawPresentationModel(it) }
    }

    fun getUserRawPresentation(): Flow<RawPresentationModel> {
        return userLocal.getMeRawPresentation()
            .map {
                RawPresentationModelSection(
                    title = it?.second?.name ?: "---",
                    subtitle = null,
                    contentIdInfo = buildList {
                        if (it?.second?.isGuestUser == false) {
                            add("Email: **${it.second.email}**")
                        }
                        add("UserID: **${config.userId}**")
                        add("DeviceID: **${config.deviceId}**")
                    },
                    rawData = it?.first?.data?.let {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    } ?: "",
                    decryptedData = it?.second?.data?.let {
                        CryptoResource.Ready(BaseRemoteModule.prettyPrintFormatter.encodeToString(it))
                    } ?: CryptoResource.WrongKeyOrData,
                    type = RawPresentationDataType.UserPresentation
                )
            }
            .map { RawPresentationModel(listOf(it)) }
    }
}

data class RawPresentationModel constructor(
    val sections: List<RawPresentationModelSection>
)

data class RawPresentationModelSection constructor(
    val title: String?,
    val subtitle: String?,
    val rawData: String,
    val contentIdInfo: List<String>,
    val decryptedData: CryptoResource<String>,
    val type: RawPresentationDataType
)