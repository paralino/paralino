package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal

class GetMemberWithLocationsUseCase(
    private val database: MainDatabaseHelper,
    private val mapper: GroupsMapperLocal,
    private val memberMapper: MemberWithLocationsMapperLocal,
    private val appPrefs: AppPrefs
) {
    suspend fun getMemberModelsWithLocationForUser(userId: String): List<MemberModelWithLocations> {
        val members = database.groupRelatedDao.getAllMembersForUserOnce(userId = userId)
        val locations = database.groupRelatedDao.getMemberLocationsForUserOnce(userId = userId)

        return members.mapNotNull { member ->
            val group = getGroupOnce(member.groupId)
                ?: return@mapNotNull null

            val memberLocations = locations.filter {
                it.memberId == member.id
            }
            memberMapper.local2Core(
                member = member,
                memberLocations = memberLocations,
                group = group
            )
        }
    }

    suspend fun getMemberWithLocationsOnce(
        groupId: String,
        userId: String
    ): MemberModelWithLocations? {
        val member = database.groupRelatedDao.getMemberOnce(
            groupId = groupId,
            userId = userId
        ) ?: return null
        val locations = database.groupRelatedDao.getMemberLocationsOnce(memberId = member.id)
        val group = getGroupOnce(groupId) ?: return null

        return memberMapper.local2Core(
            member = member,
            memberLocations = locations,
            group = group
        )
    }

    suspend fun getCurrentLocationIdForGroup(groupId: GroupOrSpaceId): String? {
        if (groupId.isPersonalSpace) {
            return database.userDeviceDao.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
                ?.takeIf { entity ->
                    entity.hasValidLocation
                }?.id
        } else {
            return database.groupRelatedDao.getSingleLocationForMemberAndDeviceOnce(
                groupId = groupId.id,
                userId = appPrefs.userId,
                deviceId = appPrefs.deviceId
            )?.id
        }
    }

    suspend fun getGroupOnce(groupId: String): UserGroup? {
        return database.groupRelatedDao.getGroupOnce(groupId = groupId)
            ?.let { mapper.local2Core(it) }
    }
}