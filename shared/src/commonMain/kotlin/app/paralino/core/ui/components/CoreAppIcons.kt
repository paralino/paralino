package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
object CoreAppIcons {
    val List get() = "drawable/ic_login.xml"
    val Check get() = "drawable/ic_check.xml"
    val PhonelinkSetup get() = "drawable/ic_phonelink_setup.xml"
    val NotificationsActive get() = "drawable/ic_notifications_active.xml"
    val LocationOff get() = "drawable/ic_location_off.xml"
    val Email get() = "drawable/ic_mail.xml"
    val WifiOff get() = "drawable/ic_wifi_off.xml"
    val Incognito get() = "drawable/ic_incognito.xml"
    val Person get() = "drawable/ic_person.xml"
    val VisibilityOff get() = "drawable/ic_visibility_off.xml"
    val Visibility get() = "drawable/ic_visibility.xml"
    val RadioButtonUnchecked get() = "drawable/ic_radio_button_unchecked.xml"
    val CheckCircle get() = "drawable/ic_check_circle.xml"
    val DragIndicator get() = "drawable/ic_drag_indicator.xml"
    val ChevronLeft get() = "drawable/ic_chevron_left.xml"
    val ChevronRight get() = "drawable/ic_chevron_right.xml"
    val Warning get() = "drawable/ic_warning.xml.xml"
    val LocationOn get() = "drawable/ic_location_on.xml.xml"
    val Group get() = "drawable/ic_group.xml"
    val Close get() = "drawable/ic_close.xml"
    val Add get() = "drawable/ic_add.xml"
    val Key get() = "drawable/ic_key.xml"
    val Save get() = "drawable/ic_save.xml"
    val ContentCopy get() = "drawable/ic_content_copy.xml"
    val Delete get() = "drawable/ic_delete.xml"
    val Share get() = "drawable/ic_share.xml"
    val AutoFixHigh get() = "drawable/ic_auto_fix_high.xml"
    val Logout get() = "drawable/ic_logout.xml"

    @Composable
    fun String.asPainterResource(): Painter {
        return painterResource(res = this)
    }
}