package app.paralino.core.presentation.settings.editprofile

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.UserMeData
import app.paralino.core.utils.Emojis

data class EditProfileScreenState constructor(
    val image: ImageModel = ImageModel.emoji(Emojis.robot),
    val name: String = "",
    val hasNameError: Boolean = false,
    val actionLoading: Boolean? = false,
) {
    companion object {
        const val profileNameLengthLimit = 100
        fun fromUser(userData: UserMeData?): EditProfileScreenState {
            return EditProfileScreenState(
                image = userData?.userImage ?: ImageModel.emoji(Emojis.robot),
                name = userData?.userName ?: ""
            )
        }
    }
}

sealed class EditProfileScreenEvent() {
    object ProfileUpdated : EditProfileScreenEvent()
}