package app.paralino.core.models

sealed class Payload<out R> {

    object Loading : Payload<Nothing>()
    data class Success<out T>(val data: T) : Payload<T>()
    data class Error<T>(val exception: Throwable) : Payload<T>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Loading -> "Loading"
        }
    }
}

val <T> Payload<T>.data: T?
    get() = (this as? Payload.Success)?.data

val Payload<*>.exception: Throwable?
    get() = (this as? Payload.Error)?.exception


inline fun <reified T, R> Payload<T>.mapSuccess(action: (T) -> R): Payload<R> {
    return when(this){
        is Payload.Success -> Payload.Success(action(data))
        is Payload.Error -> Payload.Error(exception)
        Payload.Loading -> Payload.Loading
    }
}

sealed class Resource<out R> {

    object NotInitialized : Resource<Nothing>()
    data class Ready<out T>(val data: T) : Resource<T>()

    override fun toString(): String {
        return when (this) {
            is Ready<*> -> "Ready[data=$data]"
            is NotInitialized -> "NotInitialized"
        }
    }
}

fun <T: Any> T.toResource(): Resource<T> {
    return Resource.Ready(this)
}

val <T> Resource<T>.data: T?
    get() = (this as? Resource.Ready)?.data



sealed class CryptoResource<out R> {

    object NotInitialized : CryptoResource<Nothing>()
    object MissingKey : CryptoResource<Nothing>()
    object WrongKeyOrData : CryptoResource<Nothing>()
    data class UnverifiedKeyOrData<out T>(val data: T) : CryptoResource<T>() {
        override fun toString(): String {
            return "UnverifiedKeyOrData:START>>>${data}<<<END"
        }
    }
    data class Ready<out T>(val data: T) : CryptoResource<T>() {
        override fun toString(): String {
            return "Ready:START>>>${data}<<<END"
        }
    }

    override fun toString(): String {
        return when (this) {
            is Ready<*> -> "Ready:START>>>${data}<<<END"
            is NotInitialized -> "NotInitialized"
            MissingKey -> "MissingKey"
            WrongKeyOrData -> "WrongKeyOrData"
            is UnverifiedKeyOrData<*> -> "UnverifiedKeyOrData(${if(data == null) "OK" else "null"})"
        }
    }
}

val <T> CryptoResource<T>.data: T?
    get() = (this as? CryptoResource.Ready)?.data ?: (this as? CryptoResource.UnverifiedKeyOrData)?.data

inline fun <reified T, R> CryptoResource<T>.mapSuccess(action: (T) -> R): CryptoResource<R> {
    return when(this){
        CryptoResource.MissingKey -> CryptoResource.MissingKey
        CryptoResource.NotInitialized -> CryptoResource.NotInitialized
        is CryptoResource.Ready -> CryptoResource.Ready(action(data))
        CryptoResource.WrongKeyOrData -> CryptoResource.WrongKeyOrData
        is CryptoResource.UnverifiedKeyOrData -> CryptoResource.UnverifiedKeyOrData(action(data))
    }
}