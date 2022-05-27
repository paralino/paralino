package com.zbapp.locationtrack.ui.invite

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.locationtrack.databinding.ElementInviteCodeBinding
import com.zbapp.locationtrack.ui.components.AppButtonState
import com.zbapp.locationtrack.ui.components.AutoSizeText
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.theme.AppTheme
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import com.zbapp.locationtrack.utils.extensions.viewBinding
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class InviteCodeListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<InviteCodeData>(context, attributeSet) {

    private val vb by viewBinding(ElementInviteCodeBinding::inflate)
    var onCopy: ((InviteCodeData) -> Unit)? = null
    var onShare: ((InviteCodeData) -> Unit)? = null
    var onDelete: ((InviteCodeData) -> Unit)? = null

    init {
        setFullWidth()
        vb.bCopy.setOnClickListener { itemData?.let { onCopy?.invoke(it) } }
        vb.bShare.setOnClickListener { itemData?.let { onShare?.invoke(it) } }
        vb.bDelete.setOnClickListener { itemData?.let { onDelete?.invoke(it) } }
    }

    override fun applyData(data: InviteCodeData) {
        vb.tvInviteCode.text = data.personalCodeData.code
        vb.tvInviteCodeValidity.text = data.personalCodeData.validity.toDateTimeUTC()
            .toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    }
}

data class InviteCodeData(
    val personalCodeData: PersonalCodeData,
) : ListItemType {
    override fun getIdentifier(): String {
        return personalCodeData.code
    }
}

@Composable
fun PersonalCodeItem(
    modifier: Modifier = Modifier,
    state: PersonalCodeData,
    deleteLoading: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    PersonalCodeItem(
        modifier = modifier,
        code = state.code,
        validity = state.validity.toDateTimeUTC()
            .toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
        deleteLoading = deleteLoading,
        onCopy = onCopy,
        onShare = onShare,
        onDelete = onDelete
    )
}

@Composable
fun PersonalCodeItem(
    modifier: Modifier = Modifier,
    code: String,
    validity: String,
    deleteLoading: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(10.dp)
    ) {
        AutoSizeText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .align(Alignment.CenterHorizontally),
            text = code.uppercase(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.size(40.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .size(20.dp)
            )
            Text(text = validity)
        }

        Spacer(modifier = Modifier.size(5.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { if(!deleteLoading) onDelete() }
            ) {
                if(deleteLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        tint = MaterialTheme.colors.onError,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

}

@Preview
@Composable
fun PersonalCodePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Box(modifier = Modifier.padding(40.dp)) {
                PersonalCodeItem(
                    modifier = Modifier.fillMaxWidth(),
                    code = "XXXX-XXXX",
                    validity = "3 June 2022",
                    deleteLoading = false,
                    onCopy = { },
                    onShare = { },
                    onDelete = { }
                )
            }
        }
    }
}