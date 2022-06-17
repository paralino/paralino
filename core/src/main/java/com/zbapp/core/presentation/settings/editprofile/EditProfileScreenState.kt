package com.zbapp.core.presentation.settings.editprofile

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.presentation.common.FormValidationResult
import com.zbapp.core.utils.Emojis

data class EditProfileScreenState(
    val image: ImageModel = ImageModel.emoji(Emojis.robot),
    val name: String = "",
    val hasNameError: Boolean = false,
    val actionLoading: Boolean? = false,
) {
    companion object {
        fun fromUser(userData: UserMeData?): EditProfileScreenState {
            return EditProfileScreenState(
                image = userData?.userImage ?: ImageModel.emoji(Emojis.robot),
                name = userData?.userName ?: ""
            )
        }
    }
}

sealed class EditProfileScreenEvent() {
    data class Error(val error: Throwable) : EditProfileScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        EditProfileScreenEvent()

    object ProfileUpdated : EditProfileScreenEvent()
}