package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
object CoreAppIcons {
    val List @Composable get() = painterResource("drawable/ic_login.xml")
    val Check @Composable get() = painterResource("drawable/ic_check.xml")
    val PhonelinkSetup @Composable get() = painterResource("drawable/ic_phonelink_setup.xml")
    val NotificationsActive @Composable get() = painterResource("drawable/ic_notifications_active.xml")
    val LocationOff @Composable get() = painterResource("drawable/ic_location_off.xml")
    val Email @Composable get() = painterResource("drawable/ic_mail.xml")
    val WifiOff @Composable get() = painterResource("drawable/ic_wifi_off.xml")
    val Incognito @Composable get() = painterResource("drawable/ic_incognito.xml")
    val VisibilityOff @Composable get() = painterResource("drawable/ic_visibility_off.xml")
    val Visibility @Composable get() = painterResource("drawable/ic_visibility.xml")
    val RadioButtonUnchecked get() = "drawable/ic_radio_button_unchecked.xml"
    val CheckCircle get() = "drawable/ic_check_circle.xml"
    val DragIndicator @Composable get() = painterResource("drawable/ic_drag_indicator.xml")
    val ChevronLeft @Composable get() = painterResource("drawable/ic_chevron_left.xml")
    val ChevronRight get() = "drawable/ic_chevron_right.xml"


    @Composable
    fun String.asPainterResource(): Painter {
        return painterResource(res = this)
    }
}