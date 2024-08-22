package app.paralino.core.models

data class AppBuildConfig constructor(
    val serverUrl: String,
    val projectId: String,
    val publicServerUrl: String,
    val isDevMode: Boolean,
    val hasPreviewFeatures: Boolean,
    val logToConsole: Boolean,
    val logErrorsToRemote: Boolean,
) {
    val endpoint = "$serverUrl/v1"
    val resourceIds = BackendResourceIDs

    companion object {
        lateinit var instance: AppBuildConfig
    }
}

enum class BackendDatabaseType(val id: String) {
    DATA(id = BackendResourceIDs.databaseDataID),
    KEYS(id = BackendResourceIDs.databaseKeysID),
    PRODUCT(id = BackendResourceIDs.databaseProductID)
}

object BackendResourceIDs {

    const val passwordResetUrl = "https://paralino.app/recover"
    const val emailConfirmationUrl = "https://paralino.app/verify"

    const val homeUrl = "https://paralino.com"
    const val sourceCodeUrl = "https://github.com/paralino"
    const val privacyPolicyUrl = "https://paralino.com/privacy"
    const val termsOfUseUrl = "https://paralino.com/terms"
    const val storeUrlGooglePlay = "https://play.google.com/store/apps/details?id=app.paralino.android"

    const val uniqueIdPlaceholder = "unique()"

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
    const val collectionGroupDeviceKeysID: String = "group_device_keys"
    const val collectionPersonalIdKeysID: String = "personal_id_keys"
    const val collectionPersonalGroupKeysID: String = "personal_group_keys"
    const val collectionUserKeysID: String = "user_keys"
    const val collectionUserDevicesID: String = "devices"
    const val collectionPushNotificationID: String = "push_notifications"
    const val collectionConfigID: String = "config"
    const val collectionUserPlansID: String = "plans"
    const val collectionBlockedID: String = "blocked"
}