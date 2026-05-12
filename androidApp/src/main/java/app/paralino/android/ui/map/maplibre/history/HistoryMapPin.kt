package app.paralino.android.ui.map.maplibre.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import app.paralino.android.ui.map.maplibre.rememberComposeImageBitmap
import app.paralino.core.ui.map.HistoryMapIcon
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.nil
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.BooleanValue
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.Source
import org.maplibre.compose.util.ClickResult

@Composable
fun MapLibreHistoryMarkerComposable(
    source: Source,
    canTapOnMarker: Boolean,
    zIndex: Float,
    filter: Expression<BooleanValue> = nil(),
    onClick: (String) -> Unit
) {
    SymbolLayer(
        id = "history-unclustered",
        source = source,
        iconImage = switch(
            condition(
                test = get("isClustered").asBoolean(),
                output = image(
                    historyMarkerIcon(
                        isAccurate = true,
                        isClustered = true
                    )
                )
            ),
            condition(
                test = get("isInaccurate").asBoolean(),
                output = image(
                    historyMarkerIcon(
                        isAccurate = false,
                        isClustered = false
                    )
                )
            ),
            fallback = image(
                historyMarkerIcon(
                    isAccurate = true,
                    isClustered = false
                )
            )
        ),
        iconAllowOverlap = const(true),
        filter = filter,
        sortKey = const(zIndex),
        onClick = if (canTapOnMarker) {
            { features ->
                features.firstOrNull()?.let { feature ->
                    feature.id?.jsonPrimitive?.content?.let { onClick(it) }
                    ClickResult.Consume
                } ?: ClickResult.Pass
            }
        } else {
            null
        },
        iconAnchor = switch(
            condition(
                test = get("isClustered").asBoolean(),
                output = const(SymbolAnchor.Center)
            ),
            fallback = const(SymbolAnchor.Bottom)
        )
    )
}

@Composable
internal fun historyMarkerIcon(
    isAccurate: Boolean,
    isClustered: Boolean
): ImageBitmap {
    return rememberComposeImageBitmap(
        isAccurate, isClustered
    ) {
        HistoryMapIcon(
            isAccurate = isAccurate,
            isClustered = isClustered
        )
    }
}