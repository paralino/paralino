/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zbapp.locationtrack.data.database

sealed class Payload<out R> {

    data class Success<out T>(val data: T) : Payload<T>()
    data class Error<T>(val exception: Throwable) : Payload<T>()
    object Loading : Payload<Nothing>()

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