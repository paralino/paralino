package com.zbapp.core.models.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe

interface IUserDataMapperCore: IDataMapperCore<UserMe, UserMeRaw>
interface IGroupDataMapperCore: IDataMapperCore<UserGroup, SimpleDocumentRaw>
interface ILocationDataMapperCore: IDataMapperCore<BaseLocationModel, SimpleDocumentRaw>