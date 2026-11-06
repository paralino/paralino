package app.paralino.core.presentation.billing

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.current
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import org.jetbrains.compose.resources.DrawableResource

data class FeatureTableState(
    val canExpand: Boolean,
    val isGroupPlanOnly: Boolean,
    val features: List<FeatureTableItemState>,
    val summary: List<FeatureTableSummary>
) {
    companion object {
        fun constructFeaturesTable(
            freeUiPlan: PlanInfoState,
            uiPlans: List<PlanInfoState>
        ): FeatureTableState {
            val plans = buildList {
                add(freeUiPlan)
                addAll(uiPlans)
            }

            return constructFeaturesTable(
                isGroupPlanOnly = false,
                plans = plans
            )
        }

        fun constructFeaturesTable(
            isGroupPlanOnly: Boolean,
            plans: List<PlanInfoState>
        ): FeatureTableState {
            val values = buildList {
                add(
                    FeatureTableItemState.Title(
                        content = MR.strings.paywall_feature_table_header.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue.Title(
                                content = it.name,
                                planType = it.type,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.TOP
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Section(
                            content = MR.strings.paywall_feature_section_groups.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            topSpacing = 0.dp,
                        )
                    )

                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_groups.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                FeatureTableItemValue.Count(
                                    content = it.config.groupsLimit.toString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = if (it.isSelected) {
                                        FeatureTableItemHighlightMode.FULL
                                    } else {
                                        FeatureTableItemHighlightMode.NONE
                                    }
                                )
                            }
                        )
                    )
                }

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_members.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Count(
                                content = it.config.membersLimit.toString(),
                                planType = it.type,
                                isPremium = it.config.isPremium,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_devices_member.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Count(
                                content = it.config.devicesLimit.toString(),
                                planType = it.type,
                                isPremium = it.config.isPremium,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_places_group.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            val highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                            if (it.config.placesLimit >= 0) {
                                FeatureTableItemValue.Count(
                                    content = it.config.placesLimit.toString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            } else {
                                FeatureTableItemValue.Text(
                                    content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            }
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_privacy_zones_group.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            val highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                            if (it.config.privacyZonesLimit >= 0) {
                                FeatureTableItemValue.Count(
                                    content = it.config.privacyZonesLimit.toString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            } else {
                                FeatureTableItemValue.Text(
                                    content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            }
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_summary_history.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            val highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }

                            if (!it.config.isGroupHistoryEnabled) {
                                FeatureTableItemValue.MoreInfo(
                                    title = it.config.adjustedPlanName,
                                    content = MR.strings.paywall_feature_history_days_minimum_info.localizedString(),
                                    planType = it.type,
                                    highlightMode = highlightMode
                                )
                            } else if (it.config.hasUnlimitedGroupHistory) {
                                FeatureTableItemValue.Text(
                                    content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            } else {
                                FeatureTableItemValue.Text(
                                    content = MR.plurals.groups_sharing_duration_type_days.localizedString(
                                        quantity = it.config.historyDaysLimit,
                                        it.config.historyDaysLimit
                                    ),
                                    planType = it.type,
                                    isPremium = it.config.isPremium,
                                    highlightMode = highlightMode
                                )
                            }
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_locate.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.config.hasLocationRequests) "✅" else "❌",
                                planType = it.type,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_performance.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.config.hasBestLocationPerformance) "✅" else "❌",
                                planType = it.type,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_battery.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.config.hasLowBatteryAlerts) "✅" else "❌",
                                planType = it.type,
                                highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_notify.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.config.hasSharingAlerts) "✅" else "❌",
                                planType = it.type,
                                highlightMode = if (isGroupPlanOnly) {
                                    FeatureTableItemHighlightMode.BOTTOM
                                } else if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                            )
                        }
                    )
                )

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_max_privacy.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                FeatureTableItemValue.Emoji(
                                    content = "✅",
                                    planType = it.type,
                                    highlightMode = if (it.isSelected) {
                                        FeatureTableItemHighlightMode.FULL
                                    } else {
                                        FeatureTableItemHighlightMode.NONE
                                    }
                                )
                            }
                        )
                    )
                }

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Section(
                            content = MR.strings.paywall_feature_section_personalSpace.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            topSpacing = 16.dp
                        )
                    )

                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_summary_history.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                val highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }

                                if (!it.config.isPersonalHistoryEnabled) {
                                    FeatureTableItemValue.Emoji(
                                        content = "❌",
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                } else if (it.config.hasUnlimitedPersonalHistory) {
                                    FeatureTableItemValue.Text(
                                        content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                        planType = it.type,
                                        isPremium = it.config.isPremium,
                                        highlightMode = highlightMode
                                    )
                                }  else {
                                    FeatureTableItemValue.Text(
                                        content = MR.plurals.groups_sharing_duration_type_days.localizedString(
                                            quantity = it.config.personalHistoryDaysLimit,
                                            it.config.personalHistoryDaysLimit
                                        ),
                                        planType = it.type,
                                        isPremium = it.config.isPremium,
                                        highlightMode = highlightMode
                                    )
                                }
                            }
                        )
                    )

                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_devices_personal.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                val highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                                if (!it.config.hasPersonalSpace) {
                                    FeatureTableItemValue.Emoji(
                                        content = "❌",
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                } else {
                                    FeatureTableItemValue.Count(
                                        content = it.config.personalDevicesLimit.toString(),
                                        planType = it.type,
                                        isPremium = it.config.isPremium,
                                        highlightMode = highlightMode
                                    )
                                }
                            }
                        )
                    )

                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_places_personal.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                val highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                                if (!it.config.hasPersonalSpace) {
                                    FeatureTableItemValue.Emoji(
                                        content = "❌",
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                } else if (it.config.personalPlacesLimit >= 0) {
                                    FeatureTableItemValue.Count(
                                        content = it.config.personalPlacesLimit.toString(),
                                        planType = it.type,
                                        isPremium = it.config.isPremium,
                                        highlightMode = highlightMode
                                    )
                                } else {
                                    FeatureTableItemValue.Text(
                                        content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                        planType = it.type,
                                        isPremium = it.config.isPremium,
                                        highlightMode = highlightMode
                                    )
                                }
                            }
                        )
                    )

                    if (DeviceType.current == DeviceType.Android) {
                        add(
                            FeatureTableItemState.Detail(
                                content = MR.strings.paywall_feature_premium_map_styles.localizedString(),
                                visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                                values = plans.map {
                                    FeatureTableItemValue.Emoji(
                                        content = if (it.config.hasPremiumMapStyles) "✅" else "❌",
                                        planType = it.type,
                                        highlightMode = if (it.isSelected) {
                                            FeatureTableItemHighlightMode.FULL
                                        } else {
                                            FeatureTableItemHighlightMode.NONE
                                        }
                                    )
                                }
                            )
                        )
                    }
                }

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_support_privacy.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                FeatureTableItemValue.Emoji(
                                    content = if (it.config.isPremium) "✅" else "❌",
                                    planType = it.type,
                                    highlightMode = if (it.isSelected) {
                                        FeatureTableItemHighlightMode.BOTTOM
                                    } else {
                                        FeatureTableItemHighlightMode.NONE
                                    }
                                )
                            }
                        )
                    )
                }
            }

            return FeatureTableState(
                canExpand = false,
                isGroupPlanOnly = isGroupPlanOnly,
                features = values,
                summary = createCollapsedSummary(plans = plans)
            )
        }

        private fun createCollapsedSummary(plans: List<PlanInfoState>): List<FeatureTableSummary> {
            val list = plans.map { plan ->
                FeatureTableSummary(
                        topSpacing = 0.dp,
                        planType = plan.type,
                        values = buildList {
                            add(
                                FeatureTableItemValue.Summary(
                                    title = MR.plurals.paywall_summary_groups.localizedString(
                                        quantity = plan.config.groupsLimit,
                                        plan.config.groupsLimit
                                    ),
                                    icon = CoreAppIcons.Groups,
                                    planType = plan.type,
                                    features = buildList {
                                        if (plan.config.isGroupHistoryEnabled) {
                                            add(
                                                FeatureTableItemValueDataPoint(
                                                    content = if (plan.config.hasUnlimitedGroupHistory) {
                                                        MR.strings.paywall_summary_history_unlimited.localizedString()
                                                    } else {
                                                        MR.plurals.paywall_summary_history_days.localizedString(
                                                            quantity = plan.config.historyDaysLimit,
                                                            plan.config.historyDaysLimit
                                                        )
                                                    },
                                                    isHighlighted = plan.config.hasUnlimitedGroupHistory
                                                )
                                            )
                                        }

                                        add(
                                            FeatureTableItemValueDataPoint(
                                                content = if (plan.config.placesLimit >= 0) {
                                                    MR.plurals.paywall_summary_places.localizedString(
                                                        quantity = plan.config.placesLimit,
                                                        plan.config.placesLimit
                                                    )
                                                } else {
                                                    MR.strings.paywall_summary_places_unlimited.localizedString()
                                                },
                                                isHighlighted = plan.config.placesLimit < 0
                                            )
                                        )

                                        add(
                                            FeatureTableItemValueDataPoint(
                                                content = if (plan.config.privacyZonesLimit >= 0) {
                                                    MR.plurals.paywall_summary_privacy_zones.localizedString(
                                                        quantity = plan.config.privacyZonesLimit,
                                                        plan.config.privacyZonesLimit
                                                    )
                                                } else {
                                                    MR.strings.paywall_summary_privacy_zones_unlimited.localizedString()
                                                },
                                                isHighlighted = plan.config.privacyZonesLimit < 0
                                            )
                                        )

                                        add(
                                            FeatureTableItemValueDataPoint(
                                                content = MR.plurals.paywall_summary_members.localizedString(
                                                    quantity = plan.config.membersLimit,
                                                    plan.config.membersLimit
                                                ),
                                                isHighlighted = false
                                            )
                                        )
                                    },
                                    highlightMode = if (plan.isSelected) {
                                        FeatureTableItemHighlightMode.FULL
                                    } else {
                                        FeatureTableItemHighlightMode.NONE
                                    }
                                )
                            )

                            if (plan.config.hasPersonalSpace) {
                                FeatureTableItemValue.Summary(
                                    title = MR.strings.paywall_summary_personal_title.localizedString(),
                                    icon = CoreAppIcons.Person,
                                    planType = plan.type,
                                    features = buildList {
                                        if (plan.config.isPersonalHistoryEnabled) {
                                            add(
                                                FeatureTableItemValueDataPoint(
                                                    content = if (plan.config.hasUnlimitedPersonalHistory) {
                                                        MR.strings.paywall_summary_history_unlimited.localizedString()
                                                    } else {
                                                        MR.plurals.paywall_summary_history_days.localizedString(
                                                            quantity = plan.config.personalHistoryDaysLimit,
                                                            plan.config.personalHistoryDaysLimit
                                                        )
                                                    },
                                                    isHighlighted = plan.config.hasUnlimitedPersonalHistory
                                                )
                                            )
                                        }

                                        if (plan.config.personalDevicesLimit > 1) {
                                            add(
                                                FeatureTableItemValueDataPoint(
                                                    content = MR.plurals.paywall_summary_devices.localizedString(
                                                        quantity = plan.config.personalDevicesLimit,
                                                        plan.config.personalDevicesLimit
                                                    ),
                                                    isHighlighted = false
                                                )
                                            )
                                        }
                                    },
                                    highlightMode = if (plan.isSelected) {
                                        FeatureTableItemHighlightMode.FULL
                                    } else {
                                        FeatureTableItemHighlightMode.NONE
                                    }
                                )
                                    .takeIf {
                                        it.features.isNotEmpty()
                                    }
                                    ?.let {
                                        add(it)
                                    }
                            }
                        }
                    )
            }

            return list
        }
    }
}

data class FeatureTableSummary(
    val topSpacing: Dp,
    val planType: PlanType,
    val values: List<FeatureTableItemValue.Summary>,
)

sealed interface FeatureTableItemState {

    val visibilityMode: FeatureTableItemVisibilityMode
    val values: List<FeatureTableItemValue>
    val isInSection: Boolean

    data class Title(
        val content: String,
        override val visibilityMode: FeatureTableItemVisibilityMode,
        override val values: List<FeatureTableItemValue>,
    ) : FeatureTableItemState {
        override val isInSection: Boolean = false
    }

    data class Section(
        val content: String,
        val topSpacing: Dp,
        override val visibilityMode: FeatureTableItemVisibilityMode,
    ) : FeatureTableItemState {
        override val values: List<FeatureTableItemValue> = emptyList()
        override val isInSection: Boolean = false
    }

    data class Detail(
        val content: String,
        override val visibilityMode: FeatureTableItemVisibilityMode,
        override val values: List<FeatureTableItemValue>,
        override val isInSection: Boolean = true
    ) : FeatureTableItemState
}

sealed interface FeatureTableItemValue {
    val planType: PlanType
    val highlightMode: FeatureTableItemHighlightMode

    data class Title(
        val content: String,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Count(
        val content: String,
        override val planType: PlanType,
        val isPremium: Boolean,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class MoreInfo(
        val title: String,
        val content: String,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Emoji(
        val content: String,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Text(
        val content: String,
        override val planType: PlanType,
        val isPremium: Boolean,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Summary(
        val title: String,
        val icon: DrawableResource,
        val features: List<FeatureTableItemValueDataPoint>,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue
}

data class FeatureTableItemValueDataPoint(
    val content: String,
    val isHighlighted: Boolean
)

enum class FeatureTableItemHighlightMode {
    NONE, FULL, TOP, BOTTOM, SINGLE
}

enum class FeatureTableItemVisibilityMode {
    VISIBLE_ALWAYS, VISIBLE_EXPANDED
}