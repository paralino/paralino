package app.paralino.core.presentation.settings

sealed interface SettingsActionItemType {
    object Navigation : SettingsActionItemType
    object Action : SettingsActionItemType
    object Link : SettingsActionItemType
    object Loading : SettingsActionItemType
    data class Switch(val checked: Boolean) : SettingsActionItemType
}