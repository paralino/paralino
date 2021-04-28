package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

object GroupsMapper: IDataMapper<UserGroupEntity, UserGroup> {
    override fun map(model: UserGroupEntity): UserGroup {
        return UserGroup(
            id = model.id,
            name = model.name,
            color = model.color,
            emoji = model.emoji,
            userIds = model.userIds
        )
    }

    override fun mapReversed(model: UserGroup): UserGroupEntity {
        return UserGroupEntity(
            id = model.id,
            name = model.name,
            color = model.color,
            emoji = model.emoji,
            userIds = model.userIds
        )
    }

}