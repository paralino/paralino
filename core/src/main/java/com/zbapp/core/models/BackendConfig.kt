package com.zbapp.core.models

data class BackendConfig constructor(
    val serverUrl: String,
    val projectId: String,

    val collectionGroupsID: String,
    val collectionLocationsID: String,
    val collectionInvitesID: String,
    val functionInviteCreateID: String,
    val functionInviteGetCodeID: String,
    val functionInviteGetUrlID: String,
    val functionInviteAcceptID: String,

    val debug: Boolean
)