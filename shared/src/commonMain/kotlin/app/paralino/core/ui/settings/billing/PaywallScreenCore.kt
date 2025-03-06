package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.presentation.settings.billing.FaqSectionItem
import app.paralino.core.presentation.settings.billing.FeatureTableItemHighlightMode
import app.paralino.core.presentation.settings.billing.FeatureTableItemState
import app.paralino.core.presentation.settings.billing.FeatureTableState
import app.paralino.core.presentation.settings.billing.PaywallScreenEvent
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoState
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.current
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.ui.theme.featureItemTitle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PaywallScreenCore(
    appState: CoreUiState,
    vm: PaywallViewModel,
    paddingValues: PaddingValues,
    onNavigateBack: (Boolean) -> Unit,
    onStartPurchase: (PaywallScreenEvent.OnStartPurchase) -> Unit,
    onRestorePurchase: (PaywallScreenEvent.OnRestorePurchase) -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PaywallScreenEvent.OnNavigateBack -> onNavigateBack(event.isSuccess)
                is PaywallScreenEvent.OnStartPurchase -> onStartPurchase(event)
                is PaywallScreenEvent.OnRestorePurchase -> onRestorePurchase(event)

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                PaywallScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog = dialog,
                        action = it
                    )
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = if (uiState.isLoading || uiState.selectedPlan == null) {
                    null
                } else {
                    stringResource(MR.strings.paywall_screen_title)
                },
                navigationIcon = if (uiState.isLoading) {
                    null
                } else {
                    { ToolbarButtons.Close { onNavigateBack(false) } }
                }
            )
        },
    ) {
        AnimatedVisibility(visible = uiState.isLoading, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(visible = !uiState.isLoading, modifier = Modifier.fillMaxSize()) {
            val uriHandler = LocalUriHandler.current
            PaywallScreen(
                valueFormatter = vm.valueFormatter,
                state = uiState,
                paddingValues = paddingValues,
                onTermsClick = { uriHandler.openUri(BackendResourceIDs.termsOfUseUrl) },
                onPrivacyClick = { uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl) },
                onEvent = { vm.onEvent(it) }
            )
        }
    }
}

@Composable
private fun PaywallScreen(
    valueFormatter: IValueFormatter,
    state: PaywallScreenState,
    paddingValues: PaddingValues,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onEvent: (PaywallScreenUiEvent) -> Unit,
) {
    var bottomCardHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedVisibility(state.selectedPlan == null) {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    text = stringResource(MR.strings.paywall_screen_header),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(state.selectedPlan != null) {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    text = stringResource(MR.strings.paywall_header_take_control),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            PlanTypePicker(
                state = state,
                onEvent = onEvent
            )

            if (state.selectedPlan != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                    BillingPeriodOptions(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 20.dp)
                            .padding(horizontal = 20.dp),
                        selectedPlan = state.selectedPlan!!,
                        onPeriodSelected = {
                            onEvent(
                                PaywallScreenUiEvent.SelectBillingPeriod(
                                    planType = state.selectedPlan!!.type,
                                    period = it
                                )
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(visible = state.selectedBillingPeriod?.isPromoDiscount == true) {
                NoticeInfoChip(
                    modifier = Modifier.padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    text = stringResource(MR.strings.paywall_special_offer_description),
                    icon = {
                        Text(text = "🎁")
                    }
                )
            }

            state.featureTableForSelectedPlan?.let {
                FeatureTable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .padding(horizontal = 20.dp),
                    table = it
                )
            }

            Column(
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SettingsActionItem(
                    name = stringResource(MR.strings.paywall_feature_privacy),
                    tag = stringResource(MR.strings.paywall_feature_privacy_description),
                    nameTextStyle = MaterialTheme.typography.featureItemTitle,
                    icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
                SettingsActionItem(
                    name = stringResource(MR.strings.paywall_feature_premium_sharing),
                    tag = stringResource(MR.strings.paywall_feature_premium_sharing_description),
                    nameTextStyle = MaterialTheme.typography.featureItemTitle,
                    icon = CoreAppIcons.StarFamily.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
                SettingsActionItem(
                    name = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices),
                    tag = stringResource(MR.strings.settings_account_guest_convert_intro_multiple_devices_tag),
                    nameTextStyle = MaterialTheme.typography.featureItemTitle,
                    icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
                SettingsActionItem(
                    name = stringResource(MR.strings.paywall_feature_control),
                    tag = stringResource(MR.strings.paywall_feature_control_description),
                    nameTextStyle = MaterialTheme.typography.featureItemTitle,
                    icon = CoreAppIcons.Settings.asPainterResource(),
                    type = SettingsActionItemType.Action
                )
            }

            PaywallScreenFeatureElements(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                valueFormatter = valueFormatter
            )

            AnimatedVisibility(visible = state.selectedBillingPeriod?.trialDurationDays?.takeIf { it > 0 } != null) {
                NoticeInfoChip(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 40.dp),
                    text = stringResource(
                        MR.strings.paywall_free_trial_notice_main,
                        "${state.selectedBillingPeriod?.trialDurationDays ?: " "}"
                    ),
                    icon = {
                        Text(text = "🎁")
                    }
                )
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 40.dp, bottom = 12.dp),
                text = stringResource(MR.strings.paywall_header_unlock_premium),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )

            FeatureTable(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                table = state.featureTable
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 40.dp, bottom = 4.dp),
                text = stringResource(MR.strings.paywall_header_user_reviews),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )

            PaywallUserReviewsSection()

            Column(
                modifier = Modifier
                    .padding(top = 30.dp)
                    .padding(bottom = 20.dp)
                    .background(
                        color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                    )
                    .padding(top = 40.dp, bottom = 40.dp)
            ) {
                PlanTypePicker(
                    state = state,
                    onEvent = onEvent
                )

                if (state.selectedPlan != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                        BillingPeriodOptions(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .padding(horizontal = 20.dp),
                            selectedPlan = state.selectedPlan!!,
                            onPeriodSelected = {
                                onEvent(
                                    PaywallScreenUiEvent.SelectBillingPeriod(
                                        planType = state.selectedPlan!!.type,
                                        period = it
                                    )
                                )
                            }
                        )
                    }
                }
            }

            if (state.faq != null) {
                FaqSection(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    faqsList = state.faq!!.faqs
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            SettingsActionItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                name = stringResource(MR.strings.setting_title_privacy),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                type = SettingsActionItemType.Link,
                onClick = onPrivacyClick
            )

            SettingsActionItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                name = stringResource(MR.strings.setting_title_terms),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                type = SettingsActionItemType.Link,
                onClick = onTermsClick
            )

            AnimatedVisibility(state.isPromoCodeSectionVisible && !state.isPurchaseButtonLoading) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.size(20.dp))
                    MainInputField(
                        text = state.promoCode,
                        hint = "Code",
                        hasError = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words,
                        focusManager = focusManager,
                        onInputChange = { onEvent(PaywallScreenUiEvent.OnChangePromoCode(it)) }
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    FilledButton(
                        style = ButtonStyle.BIG,
                        state = AppButtonState.loadingState(state.isPurchaseButtonLoading),
                        onClick = { onEvent(PaywallScreenUiEvent.ApplyPromoCode) }
                    ) {
                        Text(text = stringResource(MR.strings.button_confirm))
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            SettingsActionItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                name = stringResource(MR.strings.paywall_button_restore),
                icon = CoreAppIcons.Sync.asPainterResource(),
                type = SettingsActionItemType.Action,
                onClick = { onEvent(PaywallScreenUiEvent.RestorePurchase) },
                onLongClick = { onEvent(PaywallScreenUiEvent.RequestPromoCode) }
            )

            Spacer(modifier = Modifier.size(8.dp))
            Spacer(modifier = Modifier.size(with(density) { bottomCardHeight.toDp() }))
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = state.selectedPlan != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(with(density) { bottomCardHeight.toDp() })
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colors.background,
                                MaterialTheme.colors.background
                            )
                        )
                    )
            )

            PaywallBottomCard(
                state = state,
                paddingValues = paddingValues,
                onStartPurchase = { onEvent(PaywallScreenUiEvent.StartPurchase) },
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        bottomCardHeight = coordinates.size.height
                    }
            )
        }
    }
}

@Composable
private fun ColumnScope.PlanTypePicker(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    onEvent: (PaywallScreenUiEvent) -> Unit
) {
    var containerWidth by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colors.surface)
            .padding(4.dp)
            .clip(RoundedCornerShape(100))
            .align(Alignment.CenterHorizontally)
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        state.plans.forEachIndexed { index, plan ->
            PlanName(
                modifier = Modifier
                    .defaultMinSize(minWidth = 100.dp)
                    .width(
                        animateDpAsState(
                            with(LocalDensity.current) {
                                if (plan.isSelected) {
                                    containerWidth.times(0.4f).toDp()
                                } else {
                                    containerWidth.times(0.3f).toDp()
                                }
                            }
                        ).value
                    )
                    .clickable {
                        onEvent(PaywallScreenUiEvent.SelectPlanType(plan.type))
                    },
                selected = plan.isSelected,
                text = plan.displayedName
            )
        }
    }

    AnimatedVisibility(
        state.selectedPlan?.isCurrentPlan == true
    ) {
        SmallActionButton(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp),
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.onBackground.copy(ContentAlpha.disabled),
            shape = RoundedCornerShape(percent = 25),
            icon = null,
            text = {
                Text(
                    text = "*" + stringResource(MR.strings.paywall_current_plan_label),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
                )
            },
            onClick = null
        )
    }
}

@Composable
private fun PaywallBottomCard(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    paddingValues: PaddingValues,
    onStartPurchase: () -> Unit,
) {
    val billingPeriodState = state.selectedBillingPeriod
    val backgroundColor = MaterialTheme.colors.background
    val contentColor = MaterialTheme.colors.onBackground
    val selectedPlan by rememberUpdatedState(state.selectedPlan)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = 12.dp,
        backgroundColor = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 12.dp)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(billingPeriodState?.trialDurationDays != null) {
                Text(
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    text = stringResource(
                        MR.strings.paywall_free_trial_notice,
                        "${billingPeriodState?.trialDurationDays ?: " "}"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.body2.copy(fontSize = 18.sp),
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }

            AnimatedVisibility(selectedPlan != null) {
                SmallActionButton(
                    modifier = Modifier.padding(bottom = 8.dp),
                    backgroundColor = Color.Transparent,
                    contentColor = animateColorAsState(if (isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark).value,
                    icon = CoreAppIcons.CheckCircle.asPainterResource(),
                    compactMode = true,
                    iconSize = 16.dp,
                    text = {
                        Text(
                            text = if (billingPeriodState?.trialDurationDays != null) {
                                stringResource(MR.strings.paywall_cancellation_notice_trial)
                            } else {
                                stringResource(MR.strings.paywall_cancellation_notice)
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    },
                    onClick = null
                )
            }

            AnimatedVisibility(selectedPlan != null) {
                Column {
                    FilledButton(
                        modifier = Modifier,
                        style = ButtonStyle.BIG,
                        state = AppButtonState.loadingState(state.isPurchaseButtonLoading),
                        onClick = onStartPurchase
                    ) {
                        Text(
                            text = if (billingPeriodState?.trialDurationDays != null) {
                                stringResource(
                                    MR.strings.paywall_start_button_free,
                                    "${billingPeriodState.trialDurationDays}"
                                )
                            } else {
                                stringResource(MR.strings.paywall_start_button_continue)
                            }.annotatedBoldCompose(),
                            style = MaterialTheme.typography.button.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedVisibility(selectedPlan == null) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = stringResource(MR.strings.paywall_select_plan_label),
                    style = MaterialTheme.typography.h5,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun PlanName(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
    )

    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colors.primary
        else Color.Transparent
    )

    Row(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(100))
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.scale(animateFloatAsState(if (selected) 1.1f else 0.8f).value),
            text = text,
            fontSize = animateFloatAsState(if (selected) 22f else 18f).value.sp,
            color = contentColor,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            }
        )
    }
}

@Composable
private fun BillingPeriodOptions(
    modifier: Modifier = Modifier,
    selectedPlan: PlanInfoState,
    onPeriodSelected: (PlanBillingPeriod) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = stringResource(MR.strings.paywall_divider_plan_duration),
                style = MaterialTheme.typography.caption
            )
            Divider(modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(color = MaterialTheme.colors.surface)
        ) {
            selectedPlan.billingPeriods.forEachIndexed { index, state ->
                if (index > 0) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color.Gray.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                }

                SubscriptionOptionItem(
                    title = state.name,
                    discountPercent = state.discountPercent,
                    originalPrice = state.price,
                    discountedPrice = state.discountedPrice,
                    monthlyEquivalent = state.pricePerMonth,
                    isPromoDiscount = state.isPromoDiscount,
                    isSelected = state.isSelected,
                    onClick = { onPeriodSelected(state.billingPeriod) }
                )
            }
        }

        AnimatedVisibility(visible = selectedPlan.selectedBillingPeriod?.pricePerMember != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                text = stringResource(
                    MR.strings.paywall_price_per_member,
                    selectedPlan.selectedBillingPeriod?.pricePerMember ?: "",
                    selectedPlan.config.membersLimit
                ).annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubscriptionOptionItem(
    title: String,
    discountPercent: String? = null,
    originalPrice: String,
    discountedPrice: String? = null,
    monthlyEquivalent: String? = null,
    isPromoDiscount: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sourceColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colors.onPrimary,
                            shape = CircleShape
                        )
                        .padding(1.dp),
                    painter = CoreAppIcons.CheckCircle.asPainterResource(),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .background(backgroundColor, shape = CircleShape)
                        .padding(2.dp),
                    painter = CoreAppIcons.RadioButtonUnchecked.asPainterResource(),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = ContentAlpha.disabled)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                lineHeight = if (DeviceType.current == DeviceType.iOS) 22.sp else 24.sp,
                color = contentColor
            )

            AnimatedVisibility(monthlyEquivalent != null) {
                Text(
                    text = monthlyEquivalent?.annotatedBoldCompose() ?: "".annotatedBoldCompose(),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = ContentAlpha.medium)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(discountPercent != null) {
                Text(
                    modifier = Modifier
                        .background(
                            color = AppColors.redMain,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp)
                        .padding(vertical = 2.dp),
                    text = stringResource(
                        MR.strings.paywall_discount_percentage_badge_text,
                        discountPercent ?: ""
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = AppColors.lightBackground
                )
            }
            Column(
                modifier = Modifier
//                    .background(
//                        color = animateColorAsState(if (isSelected) AppColors.lightBackground else backgroundColor).value,
//                        shape = if (discountPercent != null) {
//                            RoundedCornerShape(
//                                topStart = 8.dp,
//                                bottomStart = 8.dp,
//                                topEnd = 0.dp,
//                                bottomEnd = 8.dp
//                            )
//                        } else {
//                            RoundedCornerShape(8.dp)
//                        }
//                    )
                    .padding(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isPromoDiscount) "🎁 $originalPrice" else originalPrice,
                    color = contentColor,
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun FeatureTable(
    modifier: Modifier = Modifier,
    table: FeatureTableState
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        val maxWidth = this.maxWidth
        val scrollState = rememberScrollState()
        var containerWidth by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onSizeChanged { size ->
                    containerWidth = size.width.toFloat()
                }
        ) {
            table.features.forEachIndexed { index, feature ->
                Box {
                    FeatureTableItem(
                        modifier = Modifier,
                        featureItem = feature,
                        nameWidth = if (feature.values.size > 2) {
                            maxWidth * 0.5f
                        } else if (feature.values.size == 2) {
                            maxWidth * 0.6f
                        } else {
                            maxWidth * 0.7f
                        },
                        valueWidth = if (feature.values.size > 2) {
                            maxWidth * 0.19f
                        } else if (feature.values.size == 2) {
                            maxWidth * 0.2f
                        } else {
                            maxWidth * 0.3f
                        }
                    )

                    if (index > 0) {
                        Divider(
                            modifier = Modifier
                                .width(with(LocalDensity.current) { containerWidth.toDp() })
                                .padding(horizontal = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureTableItem(
    modifier: Modifier = Modifier,
    featureItem: FeatureTableItemState,
    nameWidth: Dp,
    valueWidth: Dp
) {
    var containerHeight by remember { mutableStateOf(0f) }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .onSizeChanged { size ->
                containerHeight = size.height.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .width(nameWidth)
                .padding(start = 8.dp, end = 8.dp)
                .padding(vertical = 4.dp),
            text = featureItem.name.annotatedBoldCompose(),
            textAlign = TextAlign.Start,
            style = if (featureItem.isTitle) {
                MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.caption.copy(lineHeight = 14.sp, hyphens = Hyphens.Auto)
            }
        )

        featureItem.values.forEach { value ->
            Box(
                modifier = Modifier
                    .background(
                        color = when (value.highlightMode) {
                            FeatureTableItemHighlightMode.TOP,
                            FeatureTableItemHighlightMode.BOTTOM,
                            FeatureTableItemHighlightMode.FULL -> AppColors.accentDark.copy(alpha = 0.3f)

                            else -> Color.Transparent
                        },
                        shape = when (value.highlightMode) {
                            FeatureTableItemHighlightMode.NONE -> RectangleShape
                            FeatureTableItemHighlightMode.FULL -> RectangleShape
                            FeatureTableItemHighlightMode.TOP -> RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp
                            )

                            FeatureTableItemHighlightMode.BOTTOM -> RoundedCornerShape(
                                bottomStart = 10.dp,
                                bottomEnd = 10.dp
                            )
                        }
                    )
                    .width(valueWidth)
                    .defaultMinSize(minHeight = with(LocalDensity.current) { containerHeight.toDp() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = value.name,
                    textAlign = TextAlign.Center,
                    style = if (featureItem.isTitle) {
                        MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
                    } else if (value.isPremium) {
                        MaterialTheme.typography.body2.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        MaterialTheme.typography.button.copy(fontWeight = FontWeight.Medium)
                    }
                )
            }
        }
    }
}

@Composable
private fun FaqSection(
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