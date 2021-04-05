package com.zbapp.locationtrack.data.mappers

interface IDataMapper<ONE, TWO> {
    fun map(model: ONE): TWO
    fun mapReversed(model: TWO): ONE
}