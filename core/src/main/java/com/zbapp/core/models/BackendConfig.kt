package com.zbapp.core.models

data class BackendConfig constructor(
    val serverUrl: String,
    val projectId: String,
    val debug: Boolean
) {
    val resourceIds = BackendResourceIDs
}

object BackendResourceIDs {
    const val collectionGroupsID: String = "groups"
    const val collectionGroupMembersID: String = "group_members"
    const val collectionPersonalCodeID: String = "personal_ids"
    const val collectionGroupKeysID: String = "group_keys"
    const val collectionGroupMembersKeysID: String = "group_member_keys"

    const val functionPersonalCodeCreateID: String = "personal_code_create"
    const val functionPersonalCodeReadID: String = "personal_code_read"
    const val functionCreateGroupID: String = "group_create"
    const val functionDeleteGroupID: String = "group_delete"
    const val functionCreateGroupMemberID: String = "group_member_create"
    const val functionDeleteGroupMemberID: String = "group_member_delete"
    const val functionUpdateGroupKeysID: String = "group_keys_update"
}