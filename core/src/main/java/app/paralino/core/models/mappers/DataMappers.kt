package app.paralino.core.models.mappers

interface IDataMapperLocal<LOCAL, REMOTE> {
    suspend fun local2Remote(model: LOCAL): REMOTE
    suspend fun remote2Local(model: REMOTE): LOCAL
}