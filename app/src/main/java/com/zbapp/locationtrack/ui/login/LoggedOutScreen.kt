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
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.navigation.AppScreen

@Composable
fun LoggedOutScreen(
    appState: MainAppState,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text("Login to share your location")
        Spacer(Modifier.size(20.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            onClick = { appState.navController.navigate(AppScreen.AppStartRoot.Login.routeSignature) }
        ) {
            Icon(imageVector = Icons.Rounded.Login, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Login")
        }
        Spacer(Modifier.size(40.dp))
    }
}