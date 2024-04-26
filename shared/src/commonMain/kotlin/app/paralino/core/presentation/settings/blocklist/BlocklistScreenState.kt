package app.paralino.core.presentation.settings.blocklist

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis

data class BlocklistScreenState(
    val groups: List<BlocklistElementState> = emptyList(),
    val users: List<BlocklistElementState> = emptyList(),
    val loading: Boolean = true,
    val confirmationDialog: MainAlertDialog? = null
) {
    val items = groups + users
    val isEmpty: Boolean = items.isEmpty()
}

data class BlocklistElementState constructor(
    val id: String,
    val name: String = "",
    val image: ImageModel = ImageModel.emoji(Emojis.lock),
    val type: BlocklistType = BlocklistType.UNKNOWN,
    val actionLoading: Boolean? = null,
)

sealed class BlockListScreenEvent {
    data class Error(val error: Throwable) : BlockListScreenEvent()
}

sealed class BlockListScreenUiEvent {
    data class UnblockUserOrGroup(val id: String, val type: BlocklistType) : BlockListScreenUiEvent()
    data class UnblockUserOrGroupConfirm(val id: String) : BlockListScreenUiEvent()
    data object HideDialog : BlockListScreenUiEvent()
}