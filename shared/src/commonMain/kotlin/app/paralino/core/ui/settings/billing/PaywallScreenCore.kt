package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.presentation.settings.billing.FaqSectionItem
import app.paralino.core.presentation.settings.billing.PaywallScreenEvent
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenStep
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration

@Composable
fun PaywallScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: PaywallViewModel,
    paddingValues: PaddingValues,
    onNavigateBack: (Boolean) -> Unit,
    onStartPurchase: (PaywallScreenEvent.OnStartPurchase) -> Unit,
    onRestorePurchase: (PaywallScreenEvent.OnRestorePurchase) -> Unit,
    openLicenceScreen: () -> Unit
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

    var isOfferDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.screenStep == PaywallScreenStep.PAYWALL) {
        delay(1500)
        isOfferDialogVisible = !uiState.hasActivePlanOnAnotherPlatform && uiState.screenStep == PaywallScreenStep.PAYWALL
    }

    if (isOfferDialogVisible) {
        uiState.subscriptionOfferDialog?.let { dialog ->
            dialog.dialogOffer.Show {
                vm.onEvent(
                    PaywallScreenUiEvent.OfferDialogAction(
                        AlertDialogAction(
                            dialog = dialog,
                            action = it
                        )
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) {
        var bottomCardHeight by remember { mutableStateOf(0) }
        val density = LocalDensity.current
        val focusManager = LocalFocusManager.current
        var buttonVisible by remember { mutableStateOf(true) }

//        LaunchedEffect(uiState.screenStep) {
//            when (uiState.screenStep) {
//                PaywallScreenStep.ONBOARDING -> {
//                    buttonVisible = false
//                    delay(1200)
//                    buttonVisible = true
//                }
//
//                else -> {
//                    buttonVisible = true
//                }
//            }
//        }

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
            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = uiState.screenStep == PaywallScreenStep.ONBOARDING,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .padding(bottom = 100.dp)
                        .padding(top = 30.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier.weight(1f))
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        text = if (uiState.screenType == BillingScreenSourceType.PERSONAL_SPACE_ONBOARDING) {
                            stringResource(MR.strings.paywall_header_feature_list_screen_solo)
                        } else {
                            stringResource(MR.strings.paywall_header_feature_list_screen_family)
                        },
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier.weight(1f))
                    FeaturesSection(modifier = Modifier.padding(vertical = 20.dp))
                    Spacer(modifier.weight(1f))
                }
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = uiState.screenStep == PaywallScreenStep.DEMO,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                        .background(
                            color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .padding(bottom = 140.dp)
                        .padding(top = 30.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier.weight(1f))
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        text = stringResource(MR.strings.paywall_header_feature_demo_screen),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier.weight(1f))

                    PaywallScreenFeatureElements(
                        modifier = Modifier
                            .padding(bottom = 20.dp, top = 20.dp)
                            .fillMaxWidth(),
                        displayTitle = false,
                        displayLocationHistory = false,
                        memberName = uiState.memberName,
                        memberImageModel = uiState.memberImageModel,
                        memberDeviceName = uiState.memberDeviceName,
                        valueFormatter = vm.valueFormatter
                    )

                    Spacer(modifier.weight(1f))
                }
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = uiState.screenStep == PaywallScreenStep.LOCATION_HISTORY,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                PaywallScreenFeatureLocationHistory(
                    modifier = Modifier
                        .fillMaxSize(),
                    paddingValues = PaddingValues(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = 100.dp
                    ),
                    content = uiState.locationHistoryStepMessage
                )
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = uiState.isLoading && uiState.screenStep == PaywallScreenStep.PAYWALL,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = !uiState.isLoading && uiState.screenStep == PaywallScreenStep.PAYWALL,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                val uriHandler = LocalUriHandler.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Top
                ) {
                    DefaultToolbar(
                        title = if (uiState.isLoading || uiState.selectedPlan == null) {
                            null
                        } else {
                            stringResource(MR.strings.paywall_screen_title)
                        },
                        navigationIcon = if (uiState.isLoading || !uiState.canCloseScreen) {
                            null
                        } else {
                            { ToolbarButtons.Close { onNavigateBack(false) } }
                        },
                        elevation = 0.dp
                    )

                    PaywallScreen(
                        valueFormatter = vm.valueFormatter,
                        state = uiState,
                        bottomCardHeight = bottomCardHeight,
                        onTermsClick = { uriHandler.openUri(BackendResourceIDs.termsOfUseUrl) },
                        onPrivacyClick = { uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl) },
                        onEvent = { vm.onEvent(it) },
                        openLicenceScreen = openLicenceScreen
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = (uiState.selectedPlan != null || uiState.screenStep != PaywallScreenStep.PAYWALL),
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
                    state = uiState,
                    paddingValues = paddingValues,
                    isButtonVisible = buttonVisible,
                    onStartPurchase = { vm.onEvent(PaywallScreenUiEvent.StartPurchase) },
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            bottomCardHeight = coordinates.size.height
                        }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PaywallScreen(
    valueFormatter: IValueFormatter,
    state: PaywallScreenState,
    bottomCardHeight: Int,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onEvent: (PaywallScreenUiEvent) -> Unit,
    openLicenceScreen: () -> Unit
) {
    val density = LocalDensity.current

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

    AnimatedVisibility(
        state.currentPlan != null
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp, top = 8.dp),
            text = stringResource(MR.strings.paywall_current_plan_label, state.currentPlanName).annotatedBoldCompose(),
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
        )
    }

    AnimatedVisibility(
        state.currentPlan == null
    ) {
        Spacer(modifier = Modifier.size(16.dp))
    }

    PlanTypePicker(
        state = state,
        onEvent = onEvent
    )

    if (state.selectedPlan != null && state.isPlatformBillingSupported) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
            BillingPeriodOptions(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .padding(horizontal = 20.dp),
                selectedPlan = state.selectedPlan,
                onPeriodSelected = {
                    onEvent(
                        PaywallScreenUiEvent.SelectBillingPeriod(
                            planType = state.selectedPlan.type,
                            period = it
                        )
                    )
                }
            )
        }
    } else {
        Spacer(modifier = Modifier.size(12.dp))
    }

//    if (!state.canMakePurchaseOnThisPlatform) {
//        NoticeInfoChip(
//            modifier = Modifier
//                .fillMaxWidth()
//                .align(Alignment.CenterHorizontally)
//                .padding(horizontal = 20.dp)
//                .padding(top = 12.dp, bottom = 12.dp),
//            text = stringResource(MR.strings.plans_management_info_wrong_platform_title),
//            icon = {
//                Icon(
//                    painter = CoreAppIcons.Warning.asPainterResource(),
//                    contentDescription = null,
//                    tint = AppColors.redMain
//                )
//            }
//        )
//    }

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        visible = !state.hasActivePlanOnAnotherPlatform && state.subscriptionNoticeForCurrentPlan != null
    ) {
        NoticeInfoChip(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            text = state.subscriptionNoticeForCurrentPlan ?: "",
            icon = {
                Icon(
                    painter = CoreAppIcons.StarFamily.asPainterResource(),
                    contentDescription = null
                )
            }
        )
    }

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        visible = state.selectedBillingPeriod?.isPromoDiscount == true && !state.hasActivePlanOnAnotherPlatform
    ) {
        NoticeInfoChip(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            text = stringResource(MR.strings.paywall_special_offer_description),
            icon = {
                Text(text = "🎁")
            }
        )
    }

    var arePlansExpanded by remember { mutableStateOf(false) }

    FeatureTable(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = animateDpAsState(if (arePlansExpanded) 10.dp else 0.dp).value)
            .padding(horizontal = 20.dp),
        table = if (arePlansExpanded) {
            state.fullFeatureTable
        } else {
            state.featureTableForSelectedPlan ?: state.fullFeatureTable
        },
        isExpanded = arePlansExpanded,
        onExpandPlans = {
            arePlansExpanded = true
        },
        onCellClick = { featureTableState, item ->
            onEvent(PaywallScreenUiEvent.OnCellItemClick(table = featureTableState, cellItem = item))
        }
    )

    if (!state.screenType.isOnboarding) {
        PaywallScreenFeatureElements(
            modifier = Modifier
                .padding(top = 30.dp)
                .background(MaterialTheme.colors.background)
                .background(
                    color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                )
                .padding(bottom = 40.dp, top = 20.dp)
                .fillMaxWidth(),
            displayTitle = true,
            displayLocationHistory = true,
            memberName = state.memberName,
            memberImageModel = state.memberImageModel,
            memberDeviceName = state.memberDeviceName,
            valueFormatter = valueFormatter
        )

        FeaturesSection(modifier = Modifier.padding(top = 20.dp))
    }

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        visible = state.selectedBillingPeriod?.trialDurationDays?.takeIf { it > 0 } != null && !state.hasActivePlanOnAnotherPlatform
    ) {
        NoticeInfoChip(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = if (!state.screenType.isOnboarding) 0.dp else 20.dp)
                .padding(top = 40.dp),
            text = stringResource(
                MR.strings.paywall_free_trial_notice_main,
                "${state.selectedBillingPeriod?.trialDurationDays ?: " "}"
            ),
            icon = {
                Text(text = "🎁")
            }
        )
    }

    if (!state.screenType.isOnboarding) {
        Spacer(Modifier.size(40.dp))
    } else {
        Spacer(Modifier.size(30.dp))
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 4.dp),
        text = stringResource(MR.strings.paywall_header_user_reviews),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
    )

    PaywallUserReviewsSection()

    if (!state.screenType.isOnboarding && state.isPlatformBillingSupported) {
        Column(
            modifier = Modifier
                .padding(top = 40.dp)
                .padding(bottom = 20.dp)
                .background(
                    color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                )
                .padding(top = 50.dp, bottom = 45.dp)
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
                        selectedPlan = state.selectedPlan,
                        onPeriodSelected = {
                            onEvent(
                                PaywallScreenUiEvent.SelectBillingPeriod(
                                    planType = state.selectedPlan.type,
                                    period = it
                                )
                            )
                        }
                    )
                }
            }
        }
    } else {
        Spacer(Modifier.size(30.dp))
    }

    if (state.faq != null) {
        FaqSection(
            modifier = Modifier.padding(horizontal = 20.dp),
            faqsList = state.faq.faqs
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


    if (state.canRestorePurchases) {
        SettingsActionItem(
            modifier = Modifier.padding(horizontal = 20.dp),
            name = stringResource(MR.strings.paywall_button_restore),
            icon = CoreAppIcons.Sync.asPainterResource(),
            type = SettingsActionItemType.Action,
            onClick = { onEvent(PaywallScreenUiEvent.RestorePurchase) }
        )
    }

    SettingsActionItem(
        modifier = Modifier.padding(horizontal = 20.dp),
        name = stringResource(MR.strings.licence_screen_title),
        icon = CoreAppIcons.LicenseTicket.asPainterResource(),
        type = SettingsActionItemType.Navigation,
        onClick = openLicenceScreen
    )

    Spacer(modifier = Modifier.size(8.dp))
    Spacer(modifier = Modifier.size(with(density) { bottomCardHeight.toDp() }))
}

@Composable
private fun FeaturesSection(
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.background)
            .align(Alignment.CenterHorizontally)
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        state.plans.forEachIndexed { _, plan ->
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
                    }
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                selected = plan.isSelected,
                text = plan.displayedName
            )
        }
    }
}

@Composable
private fun PaywallBottomCard(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    isButtonVisible: Boolean,
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
            AnimatedVisibility(billingPeriodState?.trialDurationDays != null && state.screenStep == PaywallScreenStep.PAYWALL && !state.hasActivePlanOnAnotherPlatform) {
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

            AnimatedVisibility(state.hasActivePlanOnAnotherPlatform) {
                SmallActionButton(
                    modifier = Modifier.padding(bottom = 8.dp),
                    backgroundColor = Color.Transparent,
                    contentColor = animateColorAsState(if (isSystemInDarkTheme()) AppColors.highlightGold else AppColors.redMain).value,
                    icon = CoreAppIcons.Warning.asPainterResource(),
                    compactMode = true,
                    iconSize = 16.dp,
                    text = {
                        Text(
                            text = stringResource(MR.strings.plans_management_info_otherPlatformPlan_message),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption
                        )
                    },
                    onClick = null
                )
            }

            AnimatedVisibility(selectedPlan != null && state.screenStep == PaywallScreenStep.PAYWALL && !state.hasActivePlanOnAnotherPlatform) {
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

            AnimatedVisibility(state.screenStep != PaywallScreenStep.PAYWALL) {
                Spacer(modifier = Modifier.size(12.dp))
            }

            AnimatedVisibility(selectedPlan != null || state.screenStep != PaywallScreenStep.PAYWALL) {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.loadingState(!isButtonVisible || state.isPurchaseButtonLoading),
                    enabled = isButtonVisible,
                    onClick = {
                        if (isButtonVisible) {
                            onStartPurchase()
                        }
                    }
                ) {
                    Text(
                        text = state.continueButtonType.buttonText.annotatedBoldCompose(),
                        style = MaterialTheme.typography.button.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    if (state.continueButtonType.icon != null) {
                        Icon(
                            painter = state.continueButtonType.icon!!.asPainterResource(),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(selectedPlan == null && state.screenStep == PaywallScreenStep.PAYWALL) {
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
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.scale(animateFloatAsState(if (selected) 1.15f else 0.8f).value),
            text = text,
            fontSize = animateFloatAsState(if (selected) 22f else 18f).value.sp,
            color = contentColor,
            fontWeight = if (selected) {
                FontWeight.ExtraBold
            } else {
                FontWeight.Bold
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
//        Row(
//            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Divider(modifier = Modifier.weight(1f))
//            Text(
//                modifier = Modifier.padding(horizontal = 12.dp),
//                text = stringResource(MR.strings.paywall_divider_plan_duration),
//                style = MaterialTheme.typography.caption
//            )
//            Divider(modifier = Modifier.weight(1f))
//        }
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

        AnimatedVisibility(
            visible = selectedPlan.selectedBillingPeriod?.pricePerMember != null
        ) {
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
    val sourceColor = if (isSelected) MaterialTheme.colors.surface else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))
    val highlightColor by animateColorAsState(if (isSystemInDarkTheme()) MaterialTheme.colors.onBackground else MaterialTheme.colors.primary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = if (isSystemInDarkTheme()) 3.dp else 2.dp,
                color = animateColorAsState(
                    if (isSelected) {
                        if (isSystemInDarkTheme()) AppColors.accentDark else MaterialTheme.colors.primary
                    } else {
                        Color.Transparent
                    }
                ).value,
                shape = RoundedCornerShape(10.dp)
            )
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
                            color = highlightColor,
                            shape = CircleShape
                        )
                        .padding(1.dp),
                    painter = CoreAppIcons.CheckCircle.asPainterResource(),
                    contentDescription = null,
                    tint = if (isSystemInDarkTheme()) MaterialTheme.colors.primary else MaterialTheme.colors.onPrimary
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
                fontWeight = FontWeight.Medium,
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

            Text(
                modifier = Modifier.padding(vertical = 4.dp),
                text = if (isPromoDiscount) "🎁 $originalPrice" else originalPrice,
                color = contentColor,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
            )
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