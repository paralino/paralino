package com.zbapp.core.models.mappers

import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData

interface IUserMapperCore : IDataMapperCore<UserMe, UserMeRaw>
interface IUserDataMapperCore : IDataMapperCore<UserMeData, UserMeDataRaw>

