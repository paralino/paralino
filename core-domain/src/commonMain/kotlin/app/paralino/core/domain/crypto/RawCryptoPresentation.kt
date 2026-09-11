package app.paralino.core.domain.crypto

import app.paralino.core.MR
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.privacyzone.nameOrDefault
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.models.user.PushProviderEndpointConfig
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RawCryptoPresentation constructor(
    private val placeLocal: PlacesLocalDataSource,
    private val privacyZoneLocal: PrivacyZonesLocalDataSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val memberLocal: LocationLocalDataSource,
    private val userLocal: UserLocalDataSource,
    private val userKeyLocal: CryptoUserKeysLocalSource,
    private val personalDevicesLocal: PersonalDevicesLocalDataSource,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val userConfigRepo: UserConfigRepository,
    private val getHistoryUseCase: GetLocationHistoryUseCase,
    private val notificationServiceManager: NotificationSocketServiceManager,
    private val appPrefs: AppPrefs,
    private val appConfig: AppConfigRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getGroupRawPresentation(groupId: String): Flow<RawPresentationModel> {
        return groupLocal.getGroupRawPresentation(groupId)
            .flatMapLatest {
                val groupModel = RawPresentationModelSection(
                    title = it.second.name.let { "**$it**" },
                    subtitle = null,
                    decryptedDataSubtitle = null,
                    contentIdInfo = buildList {
                        // only allow group owner to see group id
                        if (it.first.ownerUserId == appPrefs.userId) {
                            add("Group ID: **$groupId**")
                        }
                        add("Group encryption scheme: **${it.first.schemeVersion}**")
                        add("Supported encryption scheme: **${AppPrefs.supportedGroupEncryptionScheme}**")
                    }.map { ContentIdItem(it) },
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
                            subtitle = null,
                            sections = buildList {
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
                    decryptedDataSubtitle = null,
                    contentIdInfo = buildList {
                        if (it.first.ownerUserId == appPrefs.userId) {
                            // only allow place owner to see place id
                            add("Place ID: **$placeId**")
                        }
                    }.map { ContentIdItem(it) },
                    rawData = it.first.data,
                    decryptedData = it.second.data.mapSuccess {
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it)
                    },
                    type = RawPresentationDataType.GroupPlacePresentation(placeId)
                )
            }
            .map { RawPresentationModel(subtitle = null, sections = listOf(it)) }
    }

    fun getGroupPrivacyZoneRawPresentation(privacyZoneId: String): Flow<RawPresentationModel> {
        return privacyZoneLocal.getPrivacyZoneRawPresentation(privacyZoneId)
            .map { zone ->
                RawPresentationModelSection(
                    title = zone.second.nameOrNull?.let { "**${zone.second.nameOrDefault}**" } ?: "---",
                    subtitle = null,
                    decryptedDataSubtitle = null,
                    contentIdInfo = buildList {
                        if (zone.first.ownerUserId == appPrefs.userId) {
                            add("Privacy zone ID: **$privacyZoneId**")
                        }
                    }.map { ContentIdItem(it) },
                    rawData = zone.first.data,
                    decryptedData = zone.second.data.mapSuccess { zoneData ->
                        BaseRemoteModule.prettyPrintFormatter.encodeToString(zoneData)
                    },
                    type = RawPresentationDataType.GroupPrivacyZonePresentation(privacyZoneId)
                )
            }
            .map { RawPresentationModel(subtitle = null, sections = listOf(it)) }
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
                    status: String?,
                    locationHistory: String?,
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
                        append("\n\n")
                        append("embedded history: ")
                        append(locationHistory)
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
                                    },
                                    locationHistory = when (it.locationHistory) {
                                        is CryptoResource.Ready -> {
                                            BaseRemoteModule.prettyPrintFormatter.encodeToString(it.locationHistory.data)
                                        }

                                        else -> it.locationHistory.toString()
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
                            decryptedDataSubtitle = null,
                            contentIdInfo = buildList {
                                if (rawPair.first.member.ownerUserId == appPrefs.userId) {
                                    add("Device ID: **${location.deviceId}**")
                                }
                            }.map { ContentIdItem(it) },
                            rawData = locationDataPrint(
                                deviceData = location.deviceData,
                                locationData = location.locationData,
                                status = location.status,
                                locationHistory = location.locationHistory,
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
                            decryptedDataSubtitle = null,
                            contentIdInfo = buildList {
                                if (rawPair.first.member.ownerUserId == appPrefs.userId) {
                                    add("User ID: **${rawPair.first.member.ownerUserId}**")
                                    add("Member ID: **${rawPair.first.member.id}**")
                                }
                                add("Devices: **${rawPair.first.locations.size}**")
                                add("Verification: **${rawPair.second.member.verificationResult}**")
                            }.map { ContentIdItem(it) },
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
            .map { RawPresentationModel(subtitle = null, sections = it) }
    }

    fun getUserRawPresentation(): Flow<RawPresentationModel> {
        return userLocal.getMeRawPresentation()
            .map {
                RawPresentationModelSection(
                    title = it?.second?.name?.let { "**$it**" } ?: "---",
                    subtitle = null,
                    decryptedDataSubtitle = null,
                    contentIdInfo = buildList {

                        add(ContentIdItem(text = "Current encryption scheme: **${userKeyLocal.getActiveLocalRawKey()?.schemeVersionType?.key ?: "Unknown"}**"))
                        add(ContentIdItem(text = "Supported encryption scheme: **${AppPrefs.supportedAccountEncryptionScheme}**"))
                        if (it?.second?.isGuestUser == false) {
                            add(ContentIdItem(text = "Email:\n**${it.second.email}**", separateWithPadding = true))
                        }
                        add(ContentIdItem(text = "User ID:\n**${appPrefs.userId}**", separateWithPadding = true))
                        add(ContentIdItem(text = "Device ID:\n**${appPrefs.deviceId}**", separateWithPadding = true))
                        add(ContentIdItem(text = "Diagnostics ID:\n**${appPrefs.errorReportingID}**", separateWithPadding = true))

                        if (appConfig.isInAppDevMode) {
                            val pushConfig = PushProviderEndpointConfig.get(
                                personalSpaceLocal.personalSpaceState.value.data?.currentDevice?.pushProviderConfig
                            )
                            val providerInfo = when (notificationServiceManager.adjustedPushProvider()) {
                                PushProvider.NOT_SET -> "not_set"
                                PushProvider.DEVICE_NATIVE -> "Native"
                                PushProvider.PARALINO -> "Paralino"
                                PushProvider.UNIFIED_PUSH -> pushConfig?.distributor ?: "UnifiedPush"
                            }
                            val info = listOf(
                                providerInfo,
                                pushConfig?.let { it.id ?: it.url } ?: appPrefs.pushTokenSynced
                            ).joinToString(separator = "\n")
                            add(ContentIdItem(text = "Push token:\n**${info}**", separateWithPadding = true))
                        }
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
            .map { RawPresentationModel(subtitle = null, sections = listOf(it)) }
    }

    fun getPersonalSpaceRawPresentation(): Flow<RawPresentationModel> {

        fun locationDataPrint(
            deviceData: String?,
            locationData: String?,
            status: String?,
            locationHistory: String?
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
                append("\n\n")
                append("embedded history: ")
                append(locationHistory)
            }
        }

        return personalDevicesLocal.getAllUserDevicesRawPresentation()
            .map { devices ->
                devices.map { rawPair ->

                    val decryptedLocationData = rawPair.second.let {
                        CryptoResource.Ready(
                            locationDataPrint(
                                deviceData = when (it.deviceData) {
                                    is CryptoResource.Ready -> {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it.deviceData.data)
                                    }

                                    else -> it.deviceData.toString()
                                },
                                locationData = when (it.location?.locationData) {
                                    is CryptoResource.Ready -> {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it.location.locationData.data)
                                    }

                                    null -> null
                                    else -> it.location.locationData.toString()
                                },
                                status = when (it.location?.status) {
                                    is CryptoResource.Ready -> {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it.location.status.data)
                                    }

                                    null -> null
                                    else -> it.location.status.toString()
                                },
                                locationHistory = when (it.location?.locationHistory) {
                                    is CryptoResource.Ready -> {
                                        BaseRemoteModule.prettyPrintFormatter.encodeToString(it.location.locationHistory.data)
                                    }

                                    null -> null
                                    else -> it.location.locationHistory.toString()
                                }
                            )
                        )
                    }

                    RawPresentationModelSection(
                        title = null,
                        subtitle = rawPair.second.deviceNameOrNull?.let {
                            MR.strings.group_member_device_title.localizedString()
                                .plus(": **$it**")
                        },
                        decryptedDataSubtitle = null,
                        contentIdInfo = buildList {
                            add("Device ID: **${rawPair.first.device.deviceId}**")
                        }.map { ContentIdItem(it) },
                        rawData = locationDataPrint(
                            deviceData = rawPair.first.device.deviceData,
                            locationData = rawPair.first.location?.locationData,
                            status = rawPair.first.location?.status,
                            locationHistory = rawPair.first.location?.locationHistory,
                        ),
                        decryptedData = decryptedLocationData,
                        type = RawPresentationDataType.PersonalSpacePresentation
                    )
                }
            }
            .map { RawPresentationModel(subtitle = null, sections = it) }
    }

    fun getLocationHistoryPointRawPresentation(
        type: RawPresentationDataType.LocationHistoryPointPresentation
    ): Flow<RawPresentationModel> {
        return getHistoryUseCase.getLocationHistoryRawPresentationByIds(
            groupOrSpaceId = type.groupOrSpaceId.asGroupOrSpaceId(),
            memberUserId = type.memberUserId,
            pointIds = type.pointIds
        ).map { result ->
            result.mapIndexed { index, sectionEntry ->
                val decryptedData = sectionEntry.second?.history?.let {
                    CryptoResource.Ready(
                        data = when (it) {
                            is CryptoResource.Ready -> {
                                BaseRemoteModule.prettyPrintFormatter.encodeToString(it.data)
                            }
                            else -> it.toString()
                        }
                    )
                } ?: CryptoResource.NotInitialized

                RawPresentationModelSection(
                    title = if (result.size > 1) "#${index.plus(1)}" else null,
                    subtitle = null,
                    decryptedDataSubtitle = sectionEntry.second?.history?.data?.history?.size?.let {
                        MR.strings.raw_data_section_history_selectedPoints_title.localizedString(it)
                    },
                    rawData = sectionEntry.first.historyData,
                    contentIdInfo = emptyList(),
                    decryptedData = decryptedData,
                    type = type
                )
            }
        }.map { sections ->
            RawPresentationModel(
                subtitle = MR.strings.raw_data_section_history_info.localizedString(),
                sections = sections
            )
        }
    }
}

data class RawPresentationModel constructor(
    val subtitle: String?,
    val sections: List<RawPresentationModelSection>
)

data class RawPresentationModelSection constructor(
    val title: String?,
    val subtitle: String?,
    val decryptedDataSubtitle: String?,
    val rawData: String,
    val contentIdInfo: List<ContentIdItem>,
    val decryptedData: CryptoResource<String>,
    val type: RawPresentationDataType
)

data class ContentIdItem(
    val text: String,
    val separateWithPadding: Boolean = false,
    val canCopy: Boolean = false
)