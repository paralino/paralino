package app.paralino.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign


@Composable
fun PasswordInputField(
    modifier: Modifier = Modifier,
    password: String,
    hint: String,
    hasError: Boolean,
    imeAction: ImeAction = ImeAction.Done,
    focusManager: FocusManager = LocalFocusManager.current,
    passwordHidden: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) },
    onInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .height(TextFieldDefaults.MinHeight),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        value = password,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions {
            if(imeAction == ImeAction.Next)
                focusManager.moveFocus(FocusDirection.Next)
            else
                focusManager.clearFocus()
        },
        visualTransformation = if (passwordHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
        placeholder = {
            Text(
                text = hint,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
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
        ),
        trailingIcon = {
            IconButton(onClick = { passwordHidden.value = !passwordHidden.value }) {
                Icon(
                    imageVector = if (passwordHidden.value) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            }
        }
    )
}