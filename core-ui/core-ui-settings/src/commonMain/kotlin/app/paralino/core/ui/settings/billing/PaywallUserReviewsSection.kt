package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PaywallUserReviewsSection(
    modifier: Modifier = Modifier
) {
    val reviews = remember { UserReviewData.reviews }

    val pagerState = rememberPagerState(pageCount = { reviews.size })

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val containerWidth = this.maxWidth
        val itemWidth = containerWidth * 0.9f
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = containerWidth * 0.05f + 20.dp),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) { page ->
                UserReviewCard(
                    review = reviews[page],
                    modifier = Modifier.width(itemWidth)
                )
            }
        }
    }
}


@Composable
private fun UserReviewCard(review: UserReviewData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = 150.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isSystemInDarkTheme()) AppColors.accentDark.copy(alpha = 0.5f) else AppColors.accentDark.copy(
                alpha = 0.8f
            ),
        ) {
            Text(
                modifier = Modifier.offset(y = (-30).dp),
                text = "❝",
                style = MaterialTheme.typography.h1.copy(fontSize = 120.sp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(review.userName),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.lightBackground.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "⭐⭐⭐⭐⭐",
                        style = MaterialTheme.typography.subtitle1,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(review.content),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.lightBackground
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// Data class for the review
data class UserReviewData(
    val userName: StringResource,
    val content: StringResource
) {
    companion object {
        val reviews = listOf(
            UserReviewData(
                userName = MR.strings.paywall_review_1_author,
                content = MR.strings.paywall_review_1_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_2_author,
                content = MR.strings.paywall_review_2_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_3_author,
                content = MR.strings.paywall_review_3_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_4_author,
                content = MR.strings.paywall_review_4_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_5_author,
                content = MR.strings.paywall_review_5_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_6_author,
                content = MR.strings.paywall_review_6_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_7_author,
                content = MR.strings.paywall_review_7_content
            ),
            UserReviewData(
                userName = MR.strings.paywall_review_8_author,
                content = MR.strings.paywall_review_8_content
            )
        )
    }
}