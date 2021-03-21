package com.zbapp.locationtrack.data

interface IDataMapper<ONE, TWO> {
    fun map(model: ONE): TWO
    fun mapReversed(model: TWO): ONE
}