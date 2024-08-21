package app.paralino.core.domain.crypto

import app.paralino.core.MR
import app.paralino.core.data.db.AppPrefs
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.provider.localizedString
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
    private val appPrefs: AppPrefs
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getGroupRawPresentation(groupId: String): Flow<RawPresentationModel> {
        return groupLocal.getGroupRawPresentation(groupId)
            .flatMapLatest {
                val groupModel = RawPresentationModelSection(
                    title = it.second.name.let { "**$it**" },
                    subtitle = null,
                    contentIdInfo = buildList {
                        // only allow group owner to see group id
                        if (it.first.ownerUserId == appPrefs.userId) {
                            add("GroupID: **$groupId**")
                        }
                        add("Group encryption scheme: **${it.first.schemeVersion}**")
                        add("Supported encryption scheme: **${AppPrefs.supportedEncryptionScheme}**")
                    },
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPresentation(groupId)
                )
                val myMemberId =
                    memberLocal.getUserMemberId(groupId = groupId, userId = appPrefs.userId) ?: ""
                getGroupMemberRawPresentation(group = it.second, memberId = myMemberId)
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
                    title = it.second.nameOrNull?.let { "**$it**" } ?: "---",
                    subtitle = null,
                    contentIdInfo = buildList {
                        if (it.first.ownerUserId == appPrefs.userId) {
                            // only allow place owner to see place id
                            add("PlaceID: **$placeId**")
                        }
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

    fun getGroupMemberRawPresentation(
        group: UserGroup?,
        memberId: String
    ): Flow<RawPresentationModel> {
        return groupLocal.getMemberRawPresentation(group, memberId)
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

                val devicesSections = rawPair.first.locations
//                    .takeIf { rawPair.first.member.ownerUserId == config.userId }
                    ?.map { location ->
                        val decryptedLocation = rawPair.second.locations
                            .firstOrNull { it.id == location.id }

                        val decryptedLocationData = decryptedLocation?.let {
                            CryptoResource.Ready(
                                locationDataPrint(
                                    deviceData = when (it.deviceData) {
                                        is CryptoResource.Ready -> {
                                            BaseRemoteModule.prettyPrintFormatter.encodeToString(it.deviceData.data)
                                        }

                                        else -> it.deviceData.toString()
                                    },
                                    locationData = when (it.locationData) {
                                        is CryptoResource.Ready -> {
                                            BaseRemoteModule.prettyPrintFormatter.encodeToString(it.locationData.data)
                                        }

                                        else -> it.locationData.toString()
                                    },
                                    status = when (it.status) {
                                        is CryptoResource.Ready -> {
                                            BaseRemoteModule.prettyPrintFormatter.encodeToString(it.status.data)
                                        }

                                        else -> it.status.toString()
                                    }
                                )
                            )
                        } ?: CryptoResource.NotInitialized

                        RawPresentationModelSection(
                            title = null,
                            subtitle = decryptedLocation?.name?.let {
                                MR.strings.group_member_device_title.localizedString()
                                    .plus(": **$it**")
                            },
                            contentIdInfo = buildList {
                                if (rawPair.first.member.ownerUserId == appPrefs.userId) {
                                    add("DeviceID: **${location.deviceId}**")
                                }
                            },
                            rawData = locationDataPrint(
                                deviceData = location.deviceData,
                                locationData = location.locationData,
                                status = location.status
                            ),
                            decryptedData = decryptedLocationData,
                            type = RawPresentationDataType.GroupMemberPresentation(memberId)
                        )
                    } ?: emptyList()

                buildList {
                    add(
                        RawPresentationModelSection(
                            title = rawPair.second.member.name.let {
                                MR.strings.group_member_title.localizedString().plus(": **$it**")
                            },
                            subtitle = null,
                            contentIdInfo = buildList {
                                if (rawPair.first.member.ownerUserId == appPrefs.userId) {
                                    add("UserID: **${appPrefs.userId}**")
                                    add("MemberID: **${rawPair.first.member.id}**")
                                }
                                add("Devices: **${rawPair.first.locations.size}**")
                                add("Verification: **${rawPair.second.member.verificationResult}**")
                            },
                            rawData = buildString {
                                append("\"data\": ")
                                append(rawPair.first.member.data)
                                append("\n\n")
                                append("\"paralinoId\": ")
                                append(rawPair.first.member.paralinoId)
                            },
                            decryptedData = CryptoResource.Ready(
                                buildString {
                                    append("\"data\": ")
                                    append(
                                        when (val memberData = rawPair.second.member.memberData) {
                                            is CryptoResource.Ready -> {
                                                BaseRemoteModule.prettyPrintFormatter.encodeToString(memberData.data)
                                            }

                                            else -> rawPair.second.member.memberData.toString()
                                        }
                                    )
                                    append("\n\n")
                                    append("\"paralinoId\": ")
                                    append(
                                        when (val paralinoId = rawPair.second.member.paralinoId) {
                                            is CryptoResource.Ready -> {
                                                BaseRemoteModule.prettyPrintFormatter.encodeToString(paralinoId.data)
                                            }

                                            else -> rawPair.second.member.paralinoId.toString()
                                        }
                                    )
                                }
                            ),
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
                    title = it?.second?.name?.let { "**$it**" } ?: "---",
                    subtitle = null,
                    contentIdInfo = buildList {
                        if (it?.second?.isGuestUser == false) {
                            add("Email: **${it.second.email}**")
                        }
                        add("UserID: **${appPrefs.userId}**")
                        add("DeviceID: **${appPrefs.deviceId}**")
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