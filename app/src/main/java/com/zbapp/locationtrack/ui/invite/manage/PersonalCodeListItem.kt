package com.zbapp.locationtrack.ui.invite.manage

import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.zbapp.core.presentation.invite.manage.PersonalCodeScreenCodeData
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.theme.AppTheme
import com.zbapp.locationtrack.utils.extensions.unixToDateTimeUTC
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PersonalCodeItem(
    modifier: Modifier = Modifier,
    state: PersonalCodeScreenCodeData,
    deleteLoading: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    PersonalCodeItem(
        modifier = modifier,
        code = state.code,
        // TODO review date format
        validity = state.validityUnix
            .unixToDateTimeUTC()
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

        val textColor = MaterialTheme.colors.primary.toArgb()
        AndroidView(
            factory = {
                AppCompatTextView(it)
                    .apply {
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            this,
                            12,
                            100,
                            2,
                            TypedValue.COMPLEX_UNIT_SP
                        )
                        this.typeface = ResourcesCompat.getFont(it, R.font.font_montserrat_bold)
                        this.maxHeight = 140.dp.value.toInt()
                        this.minHeight = 20.dp.value.toInt()
                        this.gravity = Gravity.CENTER
                        this.letterSpacing = 0.1.sp.value
                        this.setTextColor(textColor)
                        this.maxLines = 1
                        this.setLines(1)
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 10.dp)
                .align(Alignment.CenterHorizontally),
            update = {
                it.text = code.uppercase()
            }
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
                onClick = { if (!deleteLoading) onDelete() }
            ) {
                if (deleteLoading) {
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