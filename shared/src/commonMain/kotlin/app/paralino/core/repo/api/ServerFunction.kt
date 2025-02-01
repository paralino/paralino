package app.paralino.core.repo.api

import app.paralino.core.models.AppBuildConfig

enum class ServerFunction constructor(
    val method: String,
    val path: String,
    val functionId: String = "api",
    val useSdk: Boolean = true
) {
    GROUP_CREATE(method = "POST", path = "/groups"),
    GROUP_DELETE(method = "DELETE", path = "/groups"),
    GROUP_UPDATE(method = "PATCH", path = "/groups"),
    GROUP_BLOCK(method = "PUT", path = "/groups/block"),
    SETTINGS_NOTIFICATIONS_BATTERY_UPDATE(method = "PATCH", path = "/groups/notifications/battery"),
    SETTINGS_NOTIFICATIONS_PLACES_UPDATE(method = "PATCH", path = "/groups/notifications/places"),

    MEMBER_CREATE(method = "PUT", path = "/members"),
    MEMBER_DELETE(method = "DELETE", path = "/members"),
    MEMBER_UPDATE(method = "PATCH", path = "/members"),
    MEMBER_BLOCK(method = "PUT", path = "/members/block"),

    LOCATION_CREATE(method = "POST", path = "/location"),
    LOCATION_DELETE(method = "DELETE", path = "/location"),

    LOCATION_FOCUS_MEMBER(method = "POST", path = "/focus/member", functionId = "ping"),
    LOCATION_FOCUS_GROUP(method = "POST", path = "/focus/group", functionId = "ping"),
    LOCATION_REQUEST_MEMBER(method = "POST", path = "/refresh/member", functionId = "ping"),

    PLACE_CREATE(method = "PUT", path = "/places"),
    PLACE_DELETE(method = "DELETE", path = "/places"),
    PLACE_UPDATE(method = "PATCH", path = "/places"),

    KEYS_UPDATE_GROUP(method = "POST", path = "/keys/group"),
    KEYS_UPDATE_MEMBERS(method = "POST", path = "/keys/member"),
    KEYS_UPDATE_LOCATIONS(method = "POST", path = "/keys/location"),
    KEYS_UPDATE_CODE(method = "POST", path = "/keys/personal_code"),
    KEYS_UPDATE_USER(method = "POST", path = "/keys/user"),

    BILLING_ID_UPDATE(method = "POST", path = "/user/create_billing_profile"),
    BILLING_REDEEM_CODE(method = "POST", path = "/redeem_code"),

    EVENT_PLACES(method = "POST", path = "/events/places"),
    EVENT_BATTERY(method = "POST", path = "/events/battery"),

    PERSONAL_CODE_CREATE(method = "POST", path = "/personal_code"),
    PERSONAL_CODE_READ_V2(method = "POST", path = "/personal_code/read"),
    PERSONAL_CODE_UPDATE(method = "PATCH", path = "/personal_code"),
    PERSONAL_CODE_DELETE(method = "DELETE", path = "/personal_code"),

    DEVICE_UPDATE(method = "PUT", path = "/user/devices"),

    ACCOUNT_DELETE(method = "DELETE", path = "/user"),
    ACCOUNT_RESET(method = "POST", path = "/user/reset"),
    ACCOUNT_CHANGE_EMAIL(method = "PATCH", path = "/user/email"),

    CONFIG(method = "GET", path = "/config", functionId = AppBuildConfig.instance.publicServerUrl, useSdk = false),
    LOGIN(method = "POST", path = "/login", functionId = AppBuildConfig.instance.publicServerUrl, useSdk = false)
    ;

}