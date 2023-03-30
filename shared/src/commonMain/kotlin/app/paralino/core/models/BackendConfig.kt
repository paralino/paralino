package app.paralino.core.models

data class BackendConfig constructor(
    val serverUrl: String,
    val projectId: String,
    val debug: Boolean,
    val canUseEmail: Boolean,
    val logToConsole: Boolean,
    val logErrorsToRemote: Boolean,
) {
    val endpoint = "$serverUrl/v1"
    val resourceIds = BackendResourceIDs
}

enum class BackendDatabaseType(val id: String) {
    DATA(id = BackendResourceIDs.databaseDataID),
    KEYS(id = BackendResourceIDs.databaseKeysID),
    PRODUCT(id = BackendResourceIDs.databaseProductID)
}

object BackendResourceIDs {

    const val passwordResetUrl = "https://paralino.app/recover"
    const val emailConfirmationUrl = "https://paralino.app/verify"

    const val databaseIdMaxLength = 36

    const val databaseKeysID = "data_keys"
    const val databaseDataID = "data_main"
    const val databaseProductID = "product"

    const val collectionGroupsID: String = "groups"
    const val collectionGroupMembersID: String = "group_members"
    const val collectionGroupSettingsID: String = "group_settings"
    const val collectionMemberLocationsID: String = "locations"
    const val collectionPersonalCodeID: String = "personal_ids"
    const val collectionLocationFocusID: String = "location_focus"
    const val collectionPlacesID: String = "places"
    const val collectionPlacesSettingsID: String = "places_settings"

    const val collectionGroupKeysID: String = "group_keys"
    const val collectionGroupMembersKeysID: String = "group_member_keys"
    const val collectionUserKeysID: String = "user_keys"
    const val collectionUserDevicesID: String = "devices"
    const val collectionPushNotificationID: String = "push_notifications"
    const val collectionConfigID: String = "config"
    const val collectionBlockedID: String = "blocked"

    const val functionPersonalCodeReadID: String = "personal_code_read"
    const val functionPersonalCodeCreateID: String = "personal_code_create"
    const val functionCreateGroupID: String = "group_create"
    const val functionDeleteGroupID: String = "group_delete"
    const val functionUpdateGroupID: String = "group_update"
    const val functionCreateGroupMemberID: String = "group_member_create"
    const val functionDeleteGroupMemberID: String = "group_member_delete"
    const val functionUpdateGroupKeysID: String = "group_keys_update"
    const val functionUpdateGroupMemberKeysID: String = "group_member_keys_update"
    const val functionGroupMemberUpdateID: String = "group_member_update"
    const val functionDeleteAccountAndData: String = "user_account_delete"
    const val functionResetAccount: String = "user_account_reset"
    const val functionUserKeysUpdate: String = "user_keys_update"
    const val functionUserDeviceUpdate: String = "user_device_update"
    const val functionPlaceCreate: String = "place_create"
    const val functionPlaceDelete: String = "place_delete"
    const val functionPlaceUpdate: String = "place_update"
    const val functionPlaceNotify: String = "place_notify"
    const val functionLocationCreate: String = "location_create"
    const val functionLocationDelete: String = "location_delete"
}