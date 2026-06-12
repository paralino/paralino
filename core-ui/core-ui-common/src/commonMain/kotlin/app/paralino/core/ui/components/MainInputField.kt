package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.theme.AppColors

@Composable
fun MainInputField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
    text: String,
    hint: String,
    hasError: Boolean,
    maxTextLength: Int? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = if (keyboardType == KeyboardType.Email) {
        KeyboardCapitalization.None
    } else {
        KeyboardCapitalization.Sentences
    },
    onKeyboardAction: () -> Unit = {},
    onInputChange: (String) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        OutlinedTextField(
            modifier = Modifier
                .wrapContentHeight(unbounded = true)
                .fillMaxWidth(),
            textStyle = textStyle,
            value = text,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = imeAction,
                keyboardType = keyboardType,
                capitalization = capitalization
            ),
            keyboardActions = KeyboardActions {
                if (imeAction == ImeAction.Next)
                    focusManager.moveFocus(FocusDirection.Next)
                else
                    focusManager.clearFocus()
                onKeyboardAction()
            },
            label = {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    text = hint,
                    textAlign = TextAlign.Start
                )
            },
            onValueChange = {
                onInputChange(it)
            },
            shape = MaterialTheme.shapes.small,
            isError = hasError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                unfocusedBorderColor = MaterialTheme.colors.surface,
                focusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            )
        )

        if (maxTextLength != null) {
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                visible = text.length > maxTextLength / 2
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 2.dp, end = 12.dp),
                    text = "${text.length}/$maxTextLength",
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.overline.copy(
                        color = if (text.length >= maxTextLength) {
                            AppColors.redMain.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        },
                        fontWeight = if (text.length >= maxTextLength) {
                            FontWeight.SemiBold
                        } else if (text.length >= maxTextLength * 0.75) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Normal
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun MainInputBasicField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
    textValue: TextFieldValue,
    hint: String,
    hasError: Boolean,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = if (keyboardType == KeyboardType.Email) {
        KeyboardCapitalization.None
    } else {
        KeyboardCapitalization.Sentences
    },
    trailingIcon: @Composable (() -> Unit)? = null,
    onKeyboardAction: () -> Unit = {},
    onInputChange: (TextFieldValue) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.outlinedTextFieldColors(
        backgroundColor = MaterialTheme.colors.surface,
        unfocusedBorderColor = MaterialTheme.colors.surface,
        focusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
    )

    BasicTextField(
        modifier = modifier,
        value = textValue,
        onValueChange = onInputChange,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        keyboardActions = KeyboardActions {
            if (imeAction == ImeAction.Next)
                focusManager.moveFocus(FocusDirection.Next)
            else
                focusManager.clearFocus()
            onKeyboardAction()
        },
        textStyle = textStyle.copy(color = MaterialTheme.colors.onSurface.copy(alpha = LocalContentAlpha.current)),
        singleLine = true,
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = textValue.text,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = {
                    Text(
                        modifier = Modifier,
                        text = hint,
                        textAlign = TextAlign.Start
                    )
                },
                label = null,
                leadingIcon = null,
                trailingIcon = trailingIcon,
                singleLine = true,
                enabled = true,
                isError = hasError,
                interactionSource = interactionSource,
                shape = shape,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            )
        },
        cursorBrush = SolidColor(colors.cursorColor(hasError).value)
    )
}