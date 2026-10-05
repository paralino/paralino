package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.theme.featureItemTitle
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PaywallFeatureBulletPointSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
//        FeatureInfoItem(delay = 0.seconds) {
        SettingsActionItem(
            name = stringResource(MR.strings.paywall_feature_privacy),
            tag = stringResource(MR.strings.paywall_feature_privacy_description),
            nameTextStyle = MaterialTheme.typography.featureItemTitle,
            icon = CoreAppIcons.Lock.asPainterResource(),
            type = SettingsActionItemType.Action
        )
//        }
//        FeatureInfoItem(delay = 150.milliseconds) {
        SettingsActionItem(
            name = stringResource(MR.strings.paywall_feature_premium_sharing),
            tag = stringResource(MR.strings.paywall_feature_premium_sharing_description),
            nameTextStyle = MaterialTheme.typography.featureItemTitle,
            icon = CoreAppIcons.StarFamily.asPainterResource(),
            type = SettingsActionItemType.Action
        )
//        }
//        FeatureInfoItem(delay = 300.milliseconds) {
        SettingsActionItem(
            name = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices),
            tag = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices_tag),
            nameTextStyle = MaterialTheme.typography.featureItemTitle,
            icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
            type = SettingsActionItemType.Action
        )
//        }
//        FeatureInfoItem(delay = 450.milliseconds) {
        SettingsActionItem(
            name = stringResource(MR.strings.paywall_feature_control),
            tag = stringResource(MR.strings.paywall_feature_control_description),
            nameTextStyle = MaterialTheme.typography.featureItemTitle,
            icon = CoreAppIcons.Settings.asPainterResource(),
            type = SettingsActionItemType.Action
        )
//        }
    }
}

//@Composable
//private fun ColumnScope.FeatureInfoItem(
//    delay: Duration,
//    content: @Composable () -> Unit
//) {
//    var itemVisible by remember { mutableStateOf(0f) }
//    LaunchedEffect(Unit) {
//        delay(delay)
//        itemVisible = 1f
//    }
//    Box(
//        modifier = Modifier.alpha(
//            animateFloatAsState(
//                itemVisible,
//                tween(durationMillis = 600)
//            ).value
//        ),
//    ) {
//        content()
//    }
//}