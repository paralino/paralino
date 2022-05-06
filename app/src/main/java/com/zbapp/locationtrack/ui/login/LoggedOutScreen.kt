package com.zbapp.locationtrack.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoggedOutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login to share your location")
        Button(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            onClick = { }
        ) {
            Icon(imageVector = Icons.Rounded.Login, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Login")
        }
        Spacer(Modifier.size(20.dp))
        Text("Have public tracking code?")
        OutlinedTextField(
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            value = TextFieldValue("HJKI-LJZU"),
            placeholder = {
                Text(
                    "hint",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onValueChange = {},
            shape = MaterialTheme.shapes.medium
        )
    }
}