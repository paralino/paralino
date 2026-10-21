package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.group.GroupValidityVerification
import app.paralino.core.domain.crypto.group.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.GroupSettings
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberDeviceStatus
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberModelParalinoIdData
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.MissingGroupKeyException

class MemberMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val cryptoCore: CryptoCore,
    private val groupVerification: GroupValidityVerification
) {

    suspend fun getMemberVerification(
        model: MemberDocumentRaw,
        groupOwnerUserId: String,
        groupKey: GroupMasterKey?
    ): VerificationResult {
        return groupVerification.verifyGroupMember(
            member = model,
            groupOwnerId = groupOwnerUserId,
            groupKey = groupKey ?: cryptoKEKRepo.getGroupKey(
                groupId = model.groupId,
                groupOwnerId = groupOwnerUserId,
                keyVersion = model.groupKeyVersion
            )
        )
    }

    suspend fun remote2Core(
        model: MemberWithLocationsRaw,
        group: UserGroup
    ): MemberModelWithLocations {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.member.groupId,
            groupOwnerId = group.ownerUserId,
            keyVersion = model.member.groupKeyVersion
        )

        if (groupKey !is GroupMasterKey.Valid) {
            logg.e(
                MissingGroupKeyException(
                    groupId = model.member.groupId,
                    keyVersion = model.member.groupKeyVersion,
                    keyError = groupKey.toString()
                )
            )
        }

        val verification = getMemberVerification(
            model = model.member,
            groupOwnerUserId = group.ownerUserId,
            groupKey = groupKey
        )

        val memberKxKey = cryptoKEKRepo.getMemberPublicKey(
            groupId = group.id,
            memberUserId = model.member.ownerUserId
        )

        // data encrypted with user senderKey DEK
        val locations = model.locations.map { location ->
            val deviceData = if (groupKey is GroupMasterKey.Valid) {
                if (location.deviceData.isNullOrBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    val deviceGroupKey = if (location.groupKeyVersion == groupKey.version) {
                        groupKey
                    } else {
                        cryptoKEKRepo.getGroupKey(
                            groupId = model.member.groupId,
                            groupOwnerId = group.ownerUserId,
                            keyVersion = location.groupKeyVersion
                        )
                    }

                    if (deviceGroupKey is GroupMasterKey.Valid) {
                        try {
                            cryptoCore.decryptDataWithKey(
                                data = location.deviceData,
                                key = deviceGroupKey.key
                            ).let {
                                BaseRemoteModule.formatter.decodeFromString<MemberDeviceData>(it)
                            }.let {
                                if (verification is VerificationResult.Verified) {
                                    CryptoResource.Ready(it)
                                } else {
                                    CryptoResource.UnverifiedKeyOrData(it)
                                }
                            }
                        } catch (e: Exception) {
                            logg.tag("tag_crypto_key_group_member")
                                .w(
                                    message = "Error decrypting device data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "location group key: ${location.groupKeyVersion} - main group key: ${groupKey.version} - used group key: ${deviceGroupKey.version}",
                                    t = DecryptionException(
                                        type = "deviceData",
                                        message = "Error decrypting device data: ${e::class}: ${e.message}",
                                        cause = e
                                    )
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    } else {
                        CryptoResource.MissingKey
                    }
                }
            } else {
                CryptoResource.MissingKey
            }

            val memberKey = cryptoKEKRepo.getMemberSenderKeyForRead(
                groupId = model.member.groupId,
                memberUserId = model.member.ownerUserId,
                locationId = location.id,
                deviceKeyVersion = location.deviceKeyVersion
            )

            val locationData = if (memberKey is GroupMemberSenderKey.ValidForRead) {
                if (location.locationData.isNullOrBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = location.locationData,
                            key = memberKey.key
                        ).let {
                            BaseRemoteModule.formatter.decodeFromString<LocationDataModel>(it)
                        }.let {
                            if (it.isMasked) {
                                CryptoResource.NotInitialized
                            } else if (verification is VerificationResult.Verified) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        if (memberKey.deviceKeyVersion != location.deviceKeyVersion) {
                            logg.w(
                                tag = "tag_crypto_key_group_member",
                                message = "Missing device key (location: ${location.deviceKeyVersion} - key: ${memberKey.deviceKeyVersion}) ${e.message}"
                            )
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .w(
                                    message = "Error decrypting location data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Member key version: ${memberKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = DecryptionException(
                                        type = "locationData",
                                        message = "Error decrypting device location data: ${e::class}: ${e.message}",
                                        cause = e
                                    )
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    }
                }
            } else {
                CryptoResource.MissingKey
            }

            val status = if (memberKey is GroupMemberSenderKey.ValidForRead) {
                if (location.status.isNullOrBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = location.status,
                            key = memberKey.key
                        ).let {
                            BaseRemoteModule.formatter.decodeFromString<MemberDeviceStatus>(it)
                        }.let {
                            if (verification is VerificationResult.Verified) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        if (memberKey.deviceKeyVersion != location.deviceKeyVersion) {
                            logg.w(
                                tag = "tag_crypto_key_group_member",
                                message = "Missing device key (location: ${location.deviceKeyVersion} - key: ${memberKey.deviceKeyVersion}) ${e.message}"
                            )
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .w(
                                    message = "Error decrypting device status data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Member key version: ${memberKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = DecryptionException(
                                        type = "status",
                                        message = "Error decrypting device status data: ${e::class}: ${e.message}",
                                        cause = e
                                    )
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    }
                }
            } else {
                CryptoResource.MissingKey
            }

            val locationHistory = if (memberKey is GroupMemberSenderKey.ValidForRead) {
                if (location.locationHistory.isNullOrBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = location.locationHistory,
                            key = memberKey.key
                        ).let {
                            BaseRemoteModule.locationHistoryFormatter.decodeFromString<LocationHistoryModel>(it)
                        }.let {
                            if (verification is VerificationResult.Verified) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        if (memberKey.deviceKeyVersion != location.deviceKeyVersion) {
                            logg.w(
                                tag = "tag_crypto_key_group_member",
                                message = "Missing device key (location: ${location.deviceKeyVersion} - key: ${memberKey.deviceKeyVersion}) ${e.message}"
                            )
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .w(
                                    message = "Error decrypting location history for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Member key version: ${memberKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = DecryptionException(
                                        type = "locationHistory",
                                        message = "Error decrypting device location history: ${e::class}: ${e.message}",
                                        cause = e
                                    )
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    }
                }
            } else {
                CryptoResource.MissingKey
            }

            MemberLocation(
                id = location.id,
                groupOrSpaceIdString = model.member.groupId,
                memberId = model.member.id,
                userId = model.member.ownerUserId,
                deviceId = location.deviceId,
                groupKeyVersion = location.groupKeyVersion,
                deviceData = deviceData,
                locationData = locationData,
                locationHistory = locationHistory,
                status = status
            )
        }

        // data encrypted with group DEK
        val memberData = getMemberData(
            model = model.member,
            verification = verification,
            groupKey = groupKey
        )

        // data encrypted with group DEK
        val paralinoIdData = if (groupKey is GroupMasterKey.Valid) {
            if (model.member.paralinoId.isNullOrBlank()) {
                CryptoResource.NotInitialized
            } else {
                try {
                    val verifiedParalinoId = cryptoCore.verifySignature(
                        data = model.member.paralinoId,
                        publicKey = (memberKxKey as GroupMemberMainPublicKey.Valid).identityKey.key
                    )

                    cryptoCore.decryptDataWithKey(
                        data = verifiedParalinoId ?: model.member.paralinoId,
                        key = groupKey.key
                    )
                        .let {
                            BaseRemoteModule.formatter.decodeFromString<MemberModelParalinoIdData>(it)
                        }
                        .let {
                            if (verification is VerificationResult.Verified && !verifiedParalinoId.isNullOrBlank()) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                } catch (e: Exception) {
                    CryptoResource.WrongKeyOrData
                }
            }
        } else {
            CryptoResource.MissingKey
        }

        val memberModel = MemberModel(
            id = model.member.id,
            groupId = model.member.groupId,
            userId = model.member.ownerUserId,
            groupKeyVersion = model.member.groupKeyVersion,
            verification = model.member.verification,
            settings = model.member.settingsRaw?.let {
                GroupSettings(
                    id = it.id,
                    groupId = it.groupId,
                    ownerUserId = it.ownerUserId,
                    memberIntegrity = it.memberIntegrity,
                    groupIntegrity = it.groupIntegrity,
                    memberIntegrityUserKeyVersion = it.memberIntegrityUserKeyVersion,
                    groupIntegrityUserKeyVersion = it.groupIntegrityUserKeyVersion,
                    batteryAlertSettings = it.batteryAlertSettings?.let {
                        try {
                            BaseRemoteModule.formatter.decodeFromString(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                )
            },
            memberData = memberData,
            paralinoId = paralinoIdData,
            verificationResult = verification,
            enforceMemberAuthentication = group.enforceMemberAuthentication,
            createdAt = model.member.createdAt
        )

        return MemberModelWithLocations(
            member = memberModel,
            locations = locations
        )
    }

    suspend fun getMemberData(
        model: MemberDocumentRaw,
        verification: VerificationResult,
        groupOwnerUserId: String? = null
    ): CryptoResource<MemberModelData> {
        val groupKey = if (groupOwnerUserId.isNullOrBlank()) {
            cryptoKEKRepo.getGroupKey(
                groupId = model.groupId,
                keyVersion = model.groupKeyVersion
            )
        } else {
            cryptoKEKRepo.getGroupKey(
                groupId = model.groupId,
                groupOwnerId = groupOwnerUserId,
                keyVersion = model.groupKeyVersion
            )
        }

        return getMemberData(
            model = model,
            verification = verification,
            groupKey = groupKey
        )
    }

    suspend fun getMemberData(
        model: MemberDocumentRaw,
        verification: VerificationResult,
        groupKey: GroupMasterKey
    ): CryptoResource<MemberModelData> {
        val memberData = if (groupKey is GroupMasterKey.Valid) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.data,
                    key = groupKey.key
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<MemberModelData>(it)
                }.let {
                    if (verification is VerificationResult.Verified) {
                        CryptoResource.Ready(it)
                    } else {
                        CryptoResource.UnverifiedKeyOrData(it)
                    }
                }
            } catch (e: Exception) {
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.MissingKey
        }

        return memberData
    }
}