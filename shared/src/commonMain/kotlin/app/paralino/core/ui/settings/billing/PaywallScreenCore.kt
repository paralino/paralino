package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.presentation.settings.billing.FaqSectionItem
import app.paralino.core.presentation.settings.billing.PaywallScreenEvent
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoState
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
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
                navigationIcon = if (uiState.isLoading || vm.sourceType == BillingScreenSourceType.ONBOARDING) {
                    null
                } else {
                    { ToolbarButtons.Close { onNavigateBack(false) } }
                },
                actions = {
                    if (!uiState.isLoading && vm.sourceType == BillingScreenSourceType.ONBOARDING) {
                        SmallActionButton(
                            modifier = Modifier.padding(end = 8.dp),
                            backgroundColor = MaterialTheme.colors.surface,
                            contentColor = MaterialTheme.colors.onSurface,
                            icon = null,
                            hasEndArrow = false,
                            text = {
                                Text(
                                    text = stringResource(MR.strings.button_skip),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                onNavigateBack(false)
                            }
                        )
                    }
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
                .padding(paddingValues)
                .padding(top = 12.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedVisibility(state.selectedPlan == null) {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    text = stringResource(MR.strings.paywall_screen_header),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.weight(1f))

            state.plans.forEachIndexed { index, plan ->
                if (index > 0) {
                    Spacer(modifier = Modifier.size(16.dp))
                }

                CompositionLocalProvider(LocalContentAlpha provides (if (state.selectedPlan == null || plan.isSelected) 1f else 0.5f)) {
                    PlanCardDetails(
                        state = plan,

                        onPeriodSelected = { onEvent(PaywallScreenUiEvent.SelectBillingPeriod(
                            planType = plan.type,
                            period = it
                        )) },
                        onClick = { onEvent(PaywallScreenUiEvent.SelectPlanType(plan.type)) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (state.faq != null) {
                FaqSection(faqsList = state.faq.faqs)
            }

            Spacer(modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.weight(1f))

            SettingsActionItem(
                name = stringResource(MR.strings.setting_title_privacy),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                type = SettingsActionItemType.Link,
                onClick = onPrivacyClick
            )

            SettingsActionItem(
                name = stringResource(MR.strings.setting_title_terms),
                icon = CoreAppIcons.PrivacyTip.asPainterResource(),
                type = SettingsActionItemType.Link,
                onClick = onTermsClick
            )

            AnimatedVisibility (state.isPromoCodeSectionVisible && !state.isPurchaseButtonLoading) {
                Column {
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
                name = stringResource(MR.strings.paywall_button_restore),
                icon = CoreAppIcons.Sync.asPainterResource(),
                type = SettingsActionItemType.Action,
                onClick = { onEvent(PaywallScreenUiEvent.RestorePurchase) },
                onLongClick = { onEvent(PaywallScreenUiEvent.RequestPromoCode) }
            )

            Spacer(modifier = Modifier.size(40.dp))
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
                    .height(with(density) { bottomCardHeight.toDp() + paddingValues.calculateBottomPadding() + paddingValues.calculateTopPadding() })
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
                onStartPurchase = { onEvent(PaywallScreenUiEvent.StartPurchase) },
                onPeriodSelected = { period ->
                    state.selectedPlan?.let { plan ->
                        onEvent(PaywallScreenUiEvent.SelectBillingPeriod(plan.type, period))
                    }
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .onGloballyPositioned { coordinates ->
                        bottomCardHeight = coordinates.size.height
                    }
            )
        }
    }
}

@Composable
private fun PlanCardDetails(
    modifier: Modifier = Modifier,
    state: PlanInfoState,
    onPeriodSelected: (PlanBillingPeriod) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .scale(animateFloatAsState(if (state.isSelected) 1.05f else 1f).value),
        elevation = if (state.isSelected) 16.dp else 2.dp,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Box {
            if (state.type.isPremium) {
                Box(modifier = Modifier.matchParentSize()) {
                    Icon(
                        painter = CoreAppIcons.StarFamily.asPainterResource(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(150.dp)
                            .matchParentSize()
                            .rotate(20f)
                            .offset(x = (40).dp, y = -(30).dp),
                        tint = AppColors.accent.copy(alpha = 0.1f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlanName(
                        selected = state.isSelected,
                        text = state.name
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedVisibility(state.isCurrentPlan) {
                        PlanInfoBadge(
                            modifier = Modifier,
                            text = stringResource(MR.strings.paywall_current_plan_label),
                            backgroundColor = Color.Transparent,
                            onClick = onClick
                        )
                    }
                }

                AnimatedVisibility(!state.isSelected && state.hasSpecialOffer) {
                    PlanInfoBadge(
                        modifier = Modifier.padding(top = 8.dp),
                        text = "${stringResource(MR.strings.paywall_special_offer_label)} 🎁",
                        backgroundColor = AppColors.accent.copy(alpha = 0.8f),
                        onClick = onClick
                    )
                }

                AnimatedVisibility(state.isSelected && state.hasSpecialOffer) {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .background(
                                color = AppColors.accent.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "🎁 ${stringResource(MR.strings.paywall_special_offer_title)}",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(MR.strings.paywall_special_offer_info).annotatedBold(),
                            style = MaterialTheme.typography.subtitle2,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                AnimatedVisibility(state.isExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.expandedBenefits,
                            style = MaterialTheme.typography.body1
                        )
                    }
                }

                AnimatedVisibility(!state.isExpanded) {
                    Text(
                        text = state.oneLineBenefits,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanInfoBadge(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    SmallActionButton(
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = AppColors.lightBackground,
        shape = RoundedCornerShape(percent = 25),
        icon = null,
        text = {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        onClick = onClick
    )
}

@Composable
private fun PaywallBottomCard(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    onStartPurchase: () -> Unit,
    onPeriodSelected: (PlanBillingPeriod) -> Unit,
) {
    val billingPeriodState = state.selectedBillingPeriod
    val backgroundColor = AppColors.highlightGold
    val contentColor = AppColors.darkBackground
    val selectedPlan by rememberUpdatedState(state.selectedPlan)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = 6.dp,
        backgroundColor = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(billingPeriodState?.trialDurationDays == null && selectedPlan?.type?.isPremium == true) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(
                        MR.strings.paywall_all_members_features_notice,
                        selectedPlan?.includedMembersPerGroup?.let { "$it" } ?: " "),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }

            AnimatedVisibility(billingPeriodState?.trialDurationDays != null) {
                Text(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = stringResource(
                        MR.strings.paywall_free_trial_notice,
                        "${billingPeriodState?.trialDurationDays ?: " "}"
                    ).annotatedBold(),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }

            AnimatedVisibility(!selectedPlan?.billingPeriods.isNullOrEmpty()) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    if (selectedPlan != null) {
                        BillingPeriodOptions(
                            modifier = Modifier.padding(bottom = 8.dp),
                            selectedPlan = selectedPlan!!,
                            onPeriodSelected = onPeriodSelected
                        )
                    }
                }
            }

            SmallActionButton(
                modifier = Modifier.padding(bottom = 8.dp),
                backgroundColor = AppColors.accentLight.copy(alpha = 0.6f),
                contentColor = contentColor,
                icon = CoreAppIcons.Security.asPainterResource(),
                iconSize = 12.dp,
                text = {
                    Text(
                        text = stringResource(MR.strings.paywall_cancellation_notice),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                },
                onClick = null
            )

            AnimatedVisibility(state.selectedPlan != null) {
                Column {
                    FilledButton(
                        style = ButtonStyle.BIG,
                        state = AppButtonState.loadingState(state.isPurchaseButtonLoading),
                        onClick = onStartPurchase
                    ) {
                        Text(
                            text = if (billingPeriodState?.trialDurationDays != null) {
                                stringResource(MR.strings.paywall_start_button_free)
                            } else {
                                stringResource(
                                    MR.strings.paywall_start_button_continue,
                                    selectedPlan?.name ?: ""
                                )
                            }.annotatedBold()
                        )
                    }
                }
            }
            AnimatedVisibility(selectedPlan == null) {
                Column {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(MR.strings.paywall_select_plan_label),
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center,
                        color = contentColor
                    )
                }
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
        else MaterialTheme.colors.onPrimary
    )

    val backgroundColor by animateColorAsState(
        if (selected) AppColors.accentDark.copy(alpha = 0.8f)
        else Color.Transparent
    )

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(100))
            .background(backgroundColor)
            .padding(
                start = animateDpAsState(if (selected) 8.dp else 0.dp).value,
                end = animateDpAsState(if (selected) 12.dp else 0.dp).value
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            modifier = Modifier.padding(end = 8.dp),
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.highlightGold,
                unselectedColor = contentColor
            )
        )
        Text(
            modifier = Modifier,
            text = text,
            fontSize = 20.sp,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FaqSection(
    faqsList: List<FaqSectionItem>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.size(32.dp))
        SettingsSectionHeader(name = stringResource(MR.strings.paywall_faq_section_title))
        Spacer(modifier = Modifier.size(8.dp))
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
                style = MaterialTheme.typography.body2
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

@Composable
private fun BillingPeriodOptions(
    modifier: Modifier = Modifier,
    selectedPlan: PlanInfoState,
    onPeriodSelected: (PlanBillingPeriod) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        selectedPlan.billingPeriods.forEachIndexed { index, state ->
            if (index > 0) {
                Divider(color = Color.Gray.copy(alpha = 0.3f))
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
}

@Composable
private fun SubscriptionOptionItem(
    title: String,
    discountPercent: String? = null,
    originalPrice: String,
    discountedPrice: String? = null,
    monthlyEquivalent: String? = null,
    isPromoDiscount: Boolean = discountedPrice != null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = LocalContentColor.current,
                unselectedColor = LocalContentColor.current
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            AnimatedVisibility(discountedPrice != null || monthlyEquivalent != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    AnimatedVisibility(discountedPrice != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                                Text(
                                    text = discountedPrice ?: "",
                                    fontSize = 16.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    AnimatedVisibility(monthlyEquivalent != null) {
                        Text(
                            text = monthlyEquivalent ?: "",
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
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
                            color = AppColors.lightBackground,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp),
                    text = stringResource(
                        MR.strings.paywall_discount_percentage_badge_text,
                        discountPercent ?: ""
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = AppColors.redMain
                )
            }
            PlanPriceBadge(
                priceText = if (isPromoDiscount) "🎁 $originalPrice" else originalPrice,
                shape = if (discountPercent != null) {
                    RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        topEnd = 0.dp,
                        bottomEnd = 8.dp
                    )
                } else {
                    RoundedCornerShape(8.dp)
                }
            )
        }
    }
}

@Composable
private fun PlanPriceBadge(
    priceText: String,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = Modifier
            .background(
                color = AppColors.lightBackground,
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = priceText,
            color = AppColors.darkBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}