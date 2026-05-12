package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.settings.resources.Res
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
@Composable
fun LicencesScreenCore(
    coreUiState: CoreUiState,
    contentPadding: PaddingValues
) {
    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }

    LibrariesContainer(
        modifier = Modifier.fillMaxSize(),
        libraries = libraries
    )
}