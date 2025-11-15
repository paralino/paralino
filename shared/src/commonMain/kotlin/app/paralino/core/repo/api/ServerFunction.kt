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

    ALERTS_PLACES_UPDATE(method = "PATCH", path = "/alerts/place"),
    ALERTS_BATTERY_UPDATE_GROUP(method = "PATCH", path = "/alerts/battery/group"),
    ALERTS_BATTERY_UPDATE_SPACE(method = "PATCH", path = "/alerts/battery/space"),

    MEMBER_CREATE(method = "PUT", path = "/members"),
    MEMBER_DELETE(method = "DELETE", path = "/members"),
    MEMBER_UPDATE(method = "PATCH", path = "/members"),
    MEMBER_BLOCK(method = "PUT", path = "/members/block"),

    LOCATION_CREATE_GROUP(method = "POST", path = "/location/group"),
    LOCATION_CREATE_PERSONAL(method = "POST", path = "/location/personal"),

    LOCATION_DELETE_GROUP(method = "DELETE", path = "/location/group"),
    LOCATION_DELETE_PERSONAL(method = "DELETE", path = "/location/personal"),

    LOCATION_FOCUS_MEMBER(method = "POST", path = "/focus/member", functionId = "ping"),
    LOCATION_FOCUS_GROUP(method = "POST", path = "/focus/group", functionId = "ping"),
    LOCATION_REQUEST_MEMBER(method = "POST", path = "/refresh/member", functionId = "ping"),

    LOCATION_FOCUS_DEVICE(method = "POST", path = "/focus/device", functionId = "ping"),
    LOCATION_FOCUS_SPACE(method = "POST", path = "/focus/space", functionId = "ping"),
    LOCATION_REQUEST_DEVICE(method = "POST", path = "/refresh/device", functionId = "ping"),

    HISTORY_CREATE(method = "POST", path = "/history"),
    HISTORY_READ_GROUP(method = "POST", path = "/history/group/read"),
    HISTORY_READ_PERSONAL(method = "POST", path = "/history/personal/read"),
    HISTORY_UPDATE_GROUP(method = "PATCH", path = "/history/group"),
    HISTORY_UPDATE_PERSONAL(method = "PATCH", path = "/history/personal"),
    HISTORY_DELETE_GROUP(method = "DELETE", path = "/history/group"),
    HISTORY_DELETE_PERSONAL(method = "DELETE", path = "/history/personal"),

    PLACE_CREATE(method = "PUT", path = "/places"),
    PLACE_DELETE(method = "DELETE", path = "/places"),
    PLACE_UPDATE(method = "PATCH", path = "/places"),

    KEYS_UPDATE_GROUP(method = "POST", path = "/keys/group"),
    KEYS_UPDATE_MEMBERS(method = "POST", path = "/keys/member"),
    KEYS_UPDATE_LOCATIONS(method = "POST", path = "/keys/location"),
    KEYS_UPDATE_CODE(method = "POST", path = "/keys/personal_code"),
    KEYS_UPDATE_USER(method = "POST", path = "/keys/user"),
    KEYS_UPDATE_SPACE(method = "POST", path = "/keys/space"),

    BILLING_ID_UPDATE(method = "POST", path = "/user/create_billing_profile"),
    BILLING_REDEEM_CODE(method = "POST", path = "/redeem_code"),

    EVENT_PLACES(method = "POST", path = "/events/places", functionId = "ping"),
    EVENT_BATTERY_GROUP(method = "POST", path = "/events/battery/group", functionId = "ping"),
    EVENT_BATTERY_SPACE(method = "POST", path = "/events/battery/space", functionId = "ping"),

    PERSONAL_CODE_CREATE(method = "POST", path = "/personal_code"),
    PERSONAL_CODE_READ_V2(method = "POST", path = "/personal_code/read"),
    PERSONAL_CODE_UPDATE(method = "PATCH", path = "/personal_code"),
    PERSONAL_CODE_DELETE(method = "DELETE", path = "/personal_code"),

    USER_DEVICE_UPDATE(method = "PUT", path = "/user/devices"),
    USER_DEVICE_DELETE(method = "DELETE", path = "/user/devices"),

    ACCOUNT_DELETE(method = "DELETE", path = "/user"),
    ACCOUNT_RESET(method = "POST", path = "/user/reset"),
    ACCOUNT_CHANGE_EMAIL(method = "PATCH", path = "/user/email"),

    CONFIG(method = "GET", path = "/config", functionId = AppBuildConfig.instance.publicServerUrl, useSdk = false),
    BILLING_CONFIG(method = "GET", path = "/billing-config", functionId = AppBuildConfig.instance.publicServerUrl, useSdk = false),
    LOGIN(method = "POST", path = "/login", functionId = AppBuildConfig.instance.publicServerUrl, useSdk = false)
    ;

}