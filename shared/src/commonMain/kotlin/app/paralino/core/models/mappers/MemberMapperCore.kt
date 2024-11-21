package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.GroupSettings
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberDeviceStatus
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.location.MemberModelParalinoIdData
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException

class MemberMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val cryptoCore: CryptoCore,
    private val groupVerification: GroupValidityVerification
) {

    suspend fun remote2Core(
        model: MemberWithLocationsRaw,
        group: UserGroup
    ): MemberModelWithLocations {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.member.groupId,
            groupOwnerId = group.ownerUserId,
            keyVersion = model.member.groupKeyVersion
        )

        if (groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.member.groupId, model.member.groupKeyVersion))

        val verification = groupVerification.verifyGroupMember(
            member = model.member,
            groupOwnerId = group.ownerUserId,
            groupKey = groupKey
        )

        // data encrypted with user senderKey DEK
        val locations = model.locations.map { location ->
            val deviceData = if (groupKey is GroupMasterKey.Valid) {
                if (location.deviceData.isNullOrBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    val deviceGroupKey = if(location.groupKeyVersion == groupKey.version) {
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
                                if(verification is VerificationResult.Verified) {
                                    CryptoResource.Ready(it)
                                } else {
                                    CryptoResource.UnverifiedKeyOrData(it)
                                }
                            }
                        } catch (e: Exception) {
                            logg.tag("tag_crypto_key_group_member")
                                .e(
                                    message = "Error decrypting device data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "location group key: ${location.groupKeyVersion} - main group key: ${groupKey.version} - used group key: ${deviceGroupKey.version}",
                                    t = e
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

            val memberKey = cryptoKEKRepo.getMemberSenderKey(
                groupId = model.member.groupId,
                memberUserId = model.member.ownerUserId,
                locationId = location.id
            )

            val locationData = if (memberKey is GroupMemberSenderKey.Valid) {
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
                            } else if(verification is VerificationResult.Verified) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        if(memberKey.deviceKeyVersion != location.deviceKeyVersion) {
                            logg.w("tag_crypto_key_group_member", "Missing device key (location: ${location.deviceKeyVersion} - key: ${memberKey.deviceKeyVersion}) ${e.message}")
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .e(
                                    message = "Error decrypting location data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Member key version: ${memberKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = e
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    }
                }
            } else {
                CryptoResource.MissingKey
            }

            val status = if (memberKey is GroupMemberSenderKey.Valid) {
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
                            if(verification is VerificationResult.Verified) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        if(memberKey.deviceKeyVersion != location.deviceKeyVersion) {
                            logg.w("tag_crypto_key_group_member", "Missing device key (location: ${location.deviceKeyVersion} - key: ${memberKey.deviceKeyVersion}) ${e.message}")
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .e(
                                    message = "Error decrypting device status data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Member key version: ${memberKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = e
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
                groupId = model.member.groupId,
                memberId = model.member.id,
                userId = model.member.ownerUserId,
                deviceId = location.deviceId,
                groupKeyVersion = location.groupKeyVersion,
                deviceData = deviceData,
                locationData = locationData,
                status = status
            )
        }

        // data encrypted with group DEK
        val memberData = if (groupKey is GroupMasterKey.Valid) {
            try {
                cryptoCore.decryptDataWithKey(
                    data = model.member.data,
                    key = groupKey.key
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<MemberModelData>(it)
                }.let {
                    if(verification is VerificationResult.Verified) {
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

        // data encrypted with group DEK
        val paralinoIdData = if (groupKey is GroupMasterKey.Valid) {
            if (model.member.paralinoId.isNullOrBlank()) {
                CryptoResource.NotInitialized
            } else {
                try {
                    cryptoCore.decryptDataWithKey(
                        data = model.member.paralinoId,
                        key = groupKey.key
                    ).let {
                        BaseRemoteModule.formatter.decodeFromString<MemberModelParalinoIdData>(it)
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
                    groupIntegrityUserKeyVersion = it.groupIntegrityUserKeyVersion
                )
            },
            memberData = memberData,
            paralinoId = paralinoIdData,
            verificationResult = verification,
            enforceMemberAuthentication = group.enforceMemberAuthentication
        )

        return MemberModelWithLocations(
            member = memberModel,
            locations = locations
        )
    }
}