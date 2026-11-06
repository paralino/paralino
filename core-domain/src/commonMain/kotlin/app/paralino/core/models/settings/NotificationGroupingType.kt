package app.paralino.core.models.settings

import app.paralino.core.resources.moko.*

enum class NotificationGroupingType(val key: String) {
    NONE("none"),
    EVERYTHING("everything"),
    TYPE("type"),
    GROUP("group"),
    PERSON("person");

    val title get() = when(this) {
        NONE -> MR.strings.setting_notificationGrouping_noGroup_title
        EVERYTHING -> MR.strings.setting_notificationGrouping_everything_title
        GROUP -> MR.strings.setting_notificationGrouping_byGroup_title
        PERSON -> MR.strings.setting_notificationGrouping_byPerson_title
        TYPE -> MR.strings.setting_notificationGrouping_byType_title
    }

    val description get() = when(this) {
        NONE -> MR.strings.setting_notificationGrouping_noGroup_info
        EVERYTHING -> MR.strings.setting_notificationGrouping_everything_info
        GROUP -> MR.strings.setting_notificationGrouping_byGroup_info
        PERSON -> MR.strings.setting_notificationGrouping_byPerson_info
        TYPE -> MR.strings.setting_notificationGrouping_byType_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}