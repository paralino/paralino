package app.paralino.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PasswordValidationElement(
    modifier: Modifier = Modifier,
    minPasswordLength: Int,
    hasMinPasswordLength: Boolean,
    hasMinPasswordStrength: Boolean,
    doPasswordsMatch: Boolean
) {
    Column(
        modifier = modifier
    ) {
        PasswordInfoElement(
            text = stringResource(MR.strings.form_rules_password_min_length, minPasswordLength),
            active = hasMinPasswordLength
        )
        PasswordInfoElement(
            text = stringResource(MR.strings.form_rules_password_min_strength),
            active = hasMinPasswordStrength
        )
        PasswordInfoElement(
            text = stringResource(MR.strings.form_rules_password_match),
            active = doPasswordsMatch
        )
    }
}

@Composable
private fun PasswordInfoElement(
    text: String,
    active: Boolean
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = if (active) CoreAppIcons.CheckCircle.asPainterResource() else CoreAppIcons.CircleOnlyOutline.asPainterResource(),
            tint = if (active) AppColors.accent else MaterialTheme.colors.secondary,
            contentDescription = null
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = text, style = MaterialTheme.typography.caption)
    }
}