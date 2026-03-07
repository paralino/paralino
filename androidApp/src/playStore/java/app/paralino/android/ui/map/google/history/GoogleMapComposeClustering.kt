package app.paralino.android.ui.map.google.history

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.compose.ComposeUiViewRenderer
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.rememberClusterManager
import com.google.maps.android.compose.rememberComposeUiViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation

class AnchoredComposeUiClusterRenderer<T : ClusterItem>(
    context: Context,
    scope: CoroutineScope,
    map: GoogleMap,
    clusterManager: ClusterManager<T>,
    viewRendererState: State<ComposeUiViewRenderer>,
    clusterContentState: State<@Composable ((Cluster<T>) -> Unit)?>,
    clusterItemContentState: State<@Composable ((T) -> Unit)?>,
): ComposeUiClusterRenderer<T>(
    context,
    scope = scope,
    map,
    clusterManager,
    viewRendererState,
    clusterContentState,
    clusterItemContentState,
) {
    override fun onBeforeClusterItemRendered(
        item: T,
        markerOptions: MarkerOptions
    ) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        (item as? LocationHistoryClusterItem)?.let {
            val offset = it.getAnchor()
            markerOptions.anchor(offset.x, offset.y)
        }
    }
}

@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
) {
    val clusterManager = rememberClusterManager<T>()
    val renderer = rememberClusterRenderer(clusterContent, clusterItemContent, clusterManager)

    SideEffect {
        clusterManager ?: return@SideEffect
        renderer ?: return@SideEffect

        if (clusterManager.renderer != renderer) {
            clusterManager.renderer = renderer
        }

        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }

    if (clusterManager != null && renderer != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val context = LocalContext.current
    val viewRendererState = rememberUpdatedState(rememberComposeUiViewRenderer())
    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null
    MapEffect(context) { map ->
        val renderer = AnchoredComposeUiClusterRenderer(
            context,
            scope = this,
            map,
            clusterManager,
            viewRendererState,
            clusterContentState,
            clusterItemContentState,
        )
        clusterRendererState.value = renderer
        awaitCancellation()
    }
    return clusterRendererState.value
}