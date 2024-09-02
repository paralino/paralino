package app.paralino.core.models.mappers

import app.paralino.core.data.db.BlocklistEntity
import app.paralino.core.data.db.SubscriptionPlanEntity
import app.paralino.core.data.db.UserEntity
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.raw.BlocklistRaw
import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.raw.UserPlanRaw
import app.paralino.core.models.user.BlocklistModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.utils.extensions.isoWithOffsetToInstant

class UserMapperLocal constructor(
    val coreMapper: UserMapperCore,
) {

    suspend fun local2Core(model: UserEntity): UserMe {
        return coreMapper.remote2Core(local2Remote(model))
    }

    suspend fun local2Core(model: BlocklistEntity): BlocklistModel {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: UserEntity): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = UserMeDataRaw(
                userImage = model.user_data_userImage,
                userName = model.user_data_userName,
                userKeyVersion = model.user_data_userKeyVersion
            )
        )
    }

    fun remote2Local(model: UserMeRaw): UserEntity {
        return UserEntity(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            user_data_userImage = model.data?.userImage,
            user_data_userName = model.data?.userName,
            user_data_userKeyVersion = model.data?.userKeyVersion,
        )
    }

    fun remote2Local(model: BlocklistRaw): BlocklistEntity {
        return BlocklistEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            resourceId = model.resourceId,
            resourceType = model.resourceType,
            data_ = model.data,
            userKeyVersion = model.userKeyVersion
        )
    }

    fun local2Remote(model: BlocklistEntity): BlocklistRaw {
        return BlocklistRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            resourceId = model.resourceId,
            resourceType = model.resourceType,
            data = model.data_,
            userKeyVersion = model.userKeyVersion
        )
    }

    fun remote2Local(model: UserPlanRaw): SubscriptionPlanEntity {
        return SubscriptionPlanEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            planType = model.planType,
            billingPeriod = model.billingPeriod.key,
            expiresAt = model.expiresAt?.toString(),
            groupsLimitOverride = model.groupsLimitOverride,
            provider = model.provider.key
        )
    }

    fun local2Remote(model: SubscriptionPlanEntity): UserPlanRaw {
        return UserPlanRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            planType = model.planType,
            billingPeriod = PlanBillingPeriod.fromKey(model.billingPeriod)
                ?: PlanBillingPeriod.ONETIME,
            expiresAt = model.expiresAt?.isoWithOffsetToInstant(),
            groupsLimitOverride = model.groupsLimitOverride,
            provider = PlanProvider.fromKey(model.provider)
        )
    }
}

