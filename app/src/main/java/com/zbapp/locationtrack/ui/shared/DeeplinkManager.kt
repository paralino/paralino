package com.zbapp.locationtrack.ui.shared

object DeeplinkManager {

    const val host = "https://myapp.com"
    const val deeplinkLogin = "$host/settings/login"
    const val deeplinkMap = "$host/map"
    const val deeplinkInvite = "$host/settings/invite"
    const val deeplinkInviteAccept = "$host/invite/{code}"

}