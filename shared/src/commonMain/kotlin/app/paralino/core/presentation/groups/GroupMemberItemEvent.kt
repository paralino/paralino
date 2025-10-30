package app.paralino.core.presentation.groups

sealed interface GroupMemberItemEvent {
    data class onItemClick(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onActionClick(val action: MemberActionItemState, val item: GroupMemberItemState): GroupMemberItemEvent
    data class onLocationRequest(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onDirectionsRequest(val action: MemberNavigateItemState, val item: GroupMemberItemState): GroupMemberItemEvent
    data class onDeleteDeviceClick(val item: GroupMemberItemState, val device: GroupMemberItemLocationState): GroupMemberItemEvent
    data class onDeviceClick(val item: GroupMemberItemState, val device: GroupMemberItemLocationState): GroupMemberItemEvent
    data class onShowLocation(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onOpenRawData(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onCopyCode(val code: String): GroupMemberItemEvent
}