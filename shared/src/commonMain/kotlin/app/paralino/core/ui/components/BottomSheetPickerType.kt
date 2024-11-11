package app.paralino.core.ui.components

import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.groups.GroupItemState

sealed interface BottomSheetPickerType {
    data class EmojiPickerBottomSheet(val emojis: List<String>) : BottomSheetPickerType
    data class GroupPickerBottomSheet(
        val onlyShowGroups: Boolean,
        val userPlan: PlanConfig?,
        val groupsLimit: Int? = userPlan?.groupsLimit,
        val createdGroups: List<GroupItemState>,
        val joinedGroups: List<GroupItemState>
    ) : BottomSheetPickerType {

        // only show plan banner if user has own groups and no premium plan
        val shouldShowPlanBanner: Boolean = !onlyShowGroups && createdGroups.isNotEmpty() && userPlan?.planTypeEnum?.isPremium == false
        val canCreateGroup: Boolean = !onlyShowGroups && createdGroups.size < (groupsLimit ?: 0 )
    }
}