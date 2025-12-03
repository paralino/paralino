package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MainInputField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
    text: String,
    hint: String,
    hasError: Boolean,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = if (keyboardType == KeyboardType.Email)
        KeyboardCapitalization.None
    else KeyboardCapitalization.Sentences,
    onKeyboardAction: () -> Unit = {},
    onInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
fun MainInputBasicField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
    text: String,
    hint: String,
    hasError: Boolean,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = if (keyboardType == KeyboardType.Email)
        KeyboardCapitalization.None
    else KeyboardCapitalization.Sentences,
    onKeyboardAction: () -> Unit = {},
    onInputChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.outlinedTextFieldColors(
        backgroundColor = MaterialTheme.colors.surface,
        unfocusedBorderColor = MaterialTheme.colors.surface,
        focusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
    )
    BasicTextField(
        modifier = modifier,
        value = text,
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
        textStyle = textStyle,
        singleLine = true,
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = text,
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
                trailingIcon = null,
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