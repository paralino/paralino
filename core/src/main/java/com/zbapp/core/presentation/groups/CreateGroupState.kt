package com.zbapp.core.presentation.groups

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.utils.Emojis

data class CreateGroupState(
    val isNewGroup: Boolean,
    val image: ImageModel,
    val name: String,
    val hasError: Boolean,
    val isLoading: Boolean
) {
    companion object {
        fun empty(): CreateGroupState {
            return CreateGroupState(
                isNewGroup = true,
                image = ImageModel.emoji(Emojis.group1.random()),
                name = "",
                hasError = false,
                isLoading = false
            )
        }
    }
}

sealed class CreateGroupScreenEvent() {
    data class Error(val error: Throwable): CreateGroupScreenEvent()
    object GroupUpdated: CreateGroupScreenEvent()
}