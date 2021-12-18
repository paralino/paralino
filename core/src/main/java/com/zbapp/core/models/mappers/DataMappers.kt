package com.zbapp.core.models.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData

interface IUserMapperCore : IDataMapperCore<UserMe, UserMeRaw>
interface IUserDataMapperCore : IDataMapperCore<UserMeData, UserMeDataRaw>
interface IGroupDataMapperCore : IDataMapperCore<UserGroup, GroupDocumentRaw>
interface IMemberMapperCore : IDataMapperCore<MemberModel, MemberDocumentRaw>