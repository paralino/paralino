package app.paralino.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MainInputField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
    text: String,
    hint: String,
    hasError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onKeyboardAction: () -> Unit = {},
    onInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        value = text,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions {
            if(imeAction == ImeAction.Next)
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