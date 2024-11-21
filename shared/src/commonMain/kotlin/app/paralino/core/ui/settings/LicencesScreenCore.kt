package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.paralino.core.ui.CoreUiState
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.LibraryPadding
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import paralino.shared.generated.resources.Res

@OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
@Composable
fun LicencesScreenCore(
    coreUiState: CoreUiState,
    contentPadding: PaddingValues
) {
    val libraries by rememberLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }

    LibrariesContainer(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        libraries = libraries,
        licenseDialogBody = {
            Text(
                text = it.licenses.joinToString { it.licenseContent ?: it.name },
                modifier = Modifier
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibrariesContainer(
    libraries: Libs?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    colors: LibraryColors = LibraryDefaults.libraryColors(),
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
    itemSpacing: Dp = 0.dp,
    header: (LazyListScope.() -> Unit)? = null,
    onLibraryClick: ((Library) -> Unit)? = null,
    licenseDialogBody: (@Composable FlowRowScope.(Library) -> Unit)? = null,
    licenseDialogConfirmText: String = "OK",
) {
    val uriHandler = LocalUriHandler.current

    val libs = libraries?.libraries ?: persistentListOf()
    val openDialog = remember { mutableStateOf<Library?>(null) }

    Libraries(
        libraries = libs,
        modifier = modifier,
        lazyListState = lazyListState,
        contentPadding = contentPadding,
        showAuthor = showAuthor,
        showVersion = showVersion,
        showLicenseBadges = showLicenseBadges,
        colors = colors,
        padding = padding,
        itemContentPadding = itemContentPadding,
        itemSpacing = itemSpacing,
        header = header,
        onLibraryClick = { library ->
            val license = library.licenses.firstOrNull()
            if (onLibraryClick != null) {
                onLibraryClick(library)
            } else if (!license?.htmlReadyLicenseContent.isNullOrBlank()) {
                openDialog.value = library
            } else if (!license?.url.isNullOrBlank()) {
                license?.url?.also {
                    try {
                        uriHandler.openUri(it)
                    } catch (t: Throwable) {
                        println("Failed to open url: ${it}")
                    }
                }
            }
        },
    )

    val library = openDialog.value
    if (library != null && licenseDialogBody != null) {
        LicenseDialog(library, colors, licenseDialogConfirmText, body = licenseDialogBody) {
            openDialog.value = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LicenseDialog(
    library: Library,
    colors: LibraryColors = LibraryDefaults.libraryColors(),
    confirmText: String = "OK",
    body: @Composable FlowRowScope.(Library) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
        content = {
            Surface(
                modifier = Modifier.padding(vertical = 30.dp),
                shape = MaterialTheme.shapes.medium,
                color = colors.backgroundColor,
                contentColor = colors.contentColor
            ) {
                Column {
                    FlowRow(
                        modifier = Modifier.verticalScroll(scrollState).padding(8.dp).weight(1f)
                    ) {
                        body(library)
                    }
                    FlowRow(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colors.dialogConfirmButtonColor,
                            )
                        ) {
                            Text(confirmText)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Libraries(
    libraries: ImmutableList<Library>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    colors: LibraryColors = LibraryDefaults.libraryColors(),
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
    itemSpacing: Dp = 0.dp,
    header: (LazyListScope.() -> Unit)? = null,
    onLibraryClick: ((Library) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        state = lazyListState,
        contentPadding = contentPadding
    ) {
        header?.invoke(this)
        libraryItems(
            libraries,
            showAuthor,
            showVersion,
            showLicenseBadges,
            colors,
            padding,
            itemContentPadding
        ) { library ->
            val license = library.licenses.firstOrNull()
            if (onLibraryClick != null) {
                onLibraryClick.invoke(library)
            } else if (!license?.url.isNullOrBlank()) {
                license?.url?.also {
                    try {
                        uriHandler.openUri(it)
                    } catch (t: Throwable) {
                        println("Failed to open url: ${it}")
                    }
                }
            }
        }
    }
}

private inline fun LazyListScope.libraryItems(
    libraries: ImmutableList<Library>,
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    colors: LibraryColors,
    padding: LibraryPadding,
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
    crossinline onLibraryClick: ((Library) -> Unit),
) {
    items(libraries) { library ->
        Library(
            library,
            showAuthor,
            showVersion,
            showLicenseBadges,
            colors,
            padding,
            itemContentPadding
        ) {
            onLibraryClick.invoke(library)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Library(
    library: Library,
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    colors: LibraryColors = LibraryDefaults.libraryColors(),
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    contentPadding: PaddingValues = LibraryDefaults.ContentPadding,
    typography: Typography = MaterialTheme.typography,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundColor)
            .clickable { onClick.invoke() }
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = library.name,
                modifier = Modifier
                    .padding(padding.namePadding)
                    .weight(1f),
                style = typography.h6,
                color = colors.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val version = library.artifactVersion
            if (version != null && showVersion) {
                Text(
                    version,
                    modifier = Modifier.padding(padding.versionPadding),
                    style = typography.body2,
                    color = colors.contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        val author = library.author
        if (showAuthor && author.isNotBlank()) {
            Text(
                text = author,
                style = typography.body2,
                color = colors.contentColor
            )
        }
        if (showLicenseBadges && library.licenses.isNotEmpty()) {
            FlowRow {
                library.licenses.forEach {
                    Text(
                        modifier = Modifier.padding(padding.badgeContentPadding),
                        text = it.name
                    )
                }
            }
        }
    }
}
