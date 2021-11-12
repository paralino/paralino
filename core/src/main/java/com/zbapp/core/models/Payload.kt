package com.zbapp.core.models

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


inline fun <reified T, R> Payload<T>.mapPayload(newData: R?): Payload<R> {
    return when(this){
        is Payload.Success -> {
            if(newData == null) Payload.Error(NullPointerException())
            else Payload.Success(newData)
        }
        is Payload.Error -> Payload.Error(exception)
        Payload.Loading -> Payload.Loading
    }
}