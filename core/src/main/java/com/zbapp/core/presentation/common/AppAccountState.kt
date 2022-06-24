package com.zbapp.core.presentation.common

sealed class AppAccountState {
    object NO_USER: AppAccountState()
    object NO_PROFILE: AppAccountState()
    object ACCOUNT_RESET: AppAccountState()
    object USER_READY: AppAccountState()
}
