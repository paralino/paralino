package com.zbapp.remote.user

import com.etebase.client.Account
import kotlinx.coroutines.flow.MutableStateFlow

class RemoteUserCache {
    var accountCache = MutableStateFlow<Account?>(null)
    val account: Account get() = accountCache.value!!

    var openAccountCache = MutableStateFlow<Account?>(null)
    val openAccount: Account get() = openAccountCache.value!!
}