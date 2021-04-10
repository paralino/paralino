package com.zbapp.remote.user

import com.etebase.client.Account
import kotlinx.coroutines.flow.MutableStateFlow

class RemoteUserCache {
    var cachedAccount = MutableStateFlow<Account?>(null)
    val account: Account get() = cachedAccount.value!!
}