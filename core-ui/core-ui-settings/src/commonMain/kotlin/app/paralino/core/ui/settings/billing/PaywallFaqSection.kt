package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.billing.FaqSectionItem
import app.paralino.core.ui.components.SettingsSectionHeader
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal fun PaywallFaqSection(
    modifier: Modifier = Modifier,
    faqsList: List<FaqSectionItem>
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(
            name = stringResource(MR.strings.paywall_faq_section_title),
            backgroundColor = Color.Transparent
        )
        Column {
            Spacer(modifier = Modifier.size(12.dp))
            faqsList.forEach { faq ->
                FaqItem(faq)
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun FaqItem(faq: FaqSectionItem) {
    var expanded by remember { mutableStateOf(faq.expanded) }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = faq.title,
                style = MaterialTheme.typography.button
            )
            AnimatedVisibility(expanded) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = faq.content,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}