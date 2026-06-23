package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.ButtonStyle


@Composable
fun PasswordInputField(
    modifier: Modifier = Modifier,
    password: String,
    hint: String,
    hasError: Boolean,
    textStyle: TextStyle = LocalTextStyle.current,
    imeAction: ImeAction = ImeAction.Done,
    focusManager: FocusManager = LocalFocusManager.current,
    passwordHidden: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) },
    onInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier
            .heightIn(min = 64.dp)
            .fillMaxWidth(),
        textStyle = textStyle,
        value = password,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions {
            if (imeAction == ImeAction.Next)
                focusManager.moveFocus(FocusDirection.Next)
            else
                focusManager.clearFocus()
        },
        visualTransformation = if (passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
        label = {
            Text(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                    .padding(vertical = 0.dp, horizontal = 4.dp),
                text = hint,
                textAlign = TextAlign.Start
            )
        },
        onValueChange = onInputChange,
        shape = MaterialTheme.shapes.small,
        isError = hasError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            unfocusedBorderColor = MaterialTheme.colors.surface,
            focusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        ),
        trailingIcon = {
            FilledButton(
                modifier = Modifier.size(32.dp),
                onClick = { passwordHidden.value = !passwordHidden.value },
                icon = if (passwordHidden.value) {
                    CoreAppIcons.Visibility.asPainterResource()
                } else {
                    CoreAppIcons.VisibilityOff.asPainterResource()
                },
                style = ButtonStyle.ICON(
                    iconSize = 24.dp
                ),
                elevation = 0.dp,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                ),
                content = null
            )
        }
    )
}