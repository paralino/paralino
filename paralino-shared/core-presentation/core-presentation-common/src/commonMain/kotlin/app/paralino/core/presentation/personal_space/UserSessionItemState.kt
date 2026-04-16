package app.paralino.core.presentation.personal_space

import app.paralino.core.models.user.UserSession

data class UserSessionItemState(
    val session: UserSession
) {
    val isSessionExpired: Boolean = session.isSessionMaybeExpired
}