package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.GroupSettings
import app.paralino.core.models.location.*
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.serialization.decodeFromString

class MemberMapperCore constructor(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val groupVerification: GroupValidityVerification
) {

    suspend fun remote2Core(
        model: MemberWithLocationsRaw,
        groupOwnerId: String
    ): MemberModelWithLocations {

        val groupKey = cryptoKEKRepo.getGroupKey(
            groupId = model.member.groupId,
            groupOwnerId = groupOwnerId,
            keyVersion = model.member.groupKeyVersion
        )

        if (groupKey !is GroupMasterKey.Valid)
            logg.e(MissingGroupKeyException(model.member.groupId, model.member.groupKeyVersion))

        val verification = groupVerification.verifyGroupMember(
            member = model.member,
            groupOwnerId = groupOwnerId,
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
                            groupOwnerId = groupOwnerId,
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
                                if(verification == VerificationResult.VERIFIED) {
                                    CryptoResource.Ready(it)
                                } else {
                                    CryptoResource.UnverifiedKeyOrData(it)
                                }
                            }
                        } catch (e: Exception) {
                            logg.tag("tag_crypto_key_group_member")
                                .e(
                                    "Error decrypting device data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                            "Group key version: ${location.groupKeyVersion} - key version: ${groupKey.version}",
                                    e
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
                            if(verification == VerificationResult.VERIFIED) {
                                CryptoResource.Ready(it)
                            } else {
                                CryptoResource.UnverifiedKeyOrData(it)
                            }
                        }
                    } catch (e: Exception) {
                        logg.tag("tag_crypto_key_group_member")
                            .e(
                                message = "Error decrypting location data for user ${model.member.ownerUserId} - member ${model.member.id} in group ${model.member.groupId}\n" +
                                        "Member key version: ${memberKey.version}",
                                t = e
                            )
                        CryptoResource.WrongKeyOrData
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
                locationData = locationData
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
                    if(verification == VerificationResult.VERIFIED) {
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
                    integrity = it.integrity
                )
            },
            memberData = memberData,
            verificationResult = verification
        )

        return MemberModelWithLocations(
            member = memberModel,
            locations = locations
        )
    }
}