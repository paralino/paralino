package app.paralino.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale

object ScaleIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val instance = remember(interactionSource) { ScaleIndicationInstance() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions

                .collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> instance.animateToPressed()
                        is HoverInteraction.Enter,
                        is FocusInteraction.Focus-> instance.animateToSelected()
                        is PressInteraction.Release,
                        is PressInteraction.Cancel,
                        is HoverInteraction.Exit,
                        is FocusInteraction.Unfocus -> instance.animateToResting()
                        else -> {
                            instance.animateToPressed()
                            instance.animateToResting()
                        }
                    }
                }
        }

        return instance
    }
}

private class ScaleIndicationInstance : IndicationInstance {
    val scaleAnimation = Animatable(1f)

    suspend fun animateToPressed() {
        scaleAnimation.animateTo(0.93f, spring())
    }

    suspend fun animateToSelected() {
        scaleAnimation.animateTo(0.96f, spring())
    }

    suspend fun animateToResting() {
        scaleAnimation.animateTo(1f, spring())
    }

    override fun ContentDrawScope.drawIndication() {
        scale(scale = scaleAnimation.value) {
            this@drawIndication.drawContent()
        }
    }
}