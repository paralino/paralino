package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.localizedString

data class FeatureTableState(
    val canExpand: Boolean,
    val isGroupPlanOnly: Boolean,
    val features: List<FeatureTableItemState>
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
                    addAll(createCollapsedSummary(plans = plans))

                    add(
                        FeatureTableItemState.Section(
                            content = MR.strings.paywall_feature_groups.localizedString(),
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
                                    highlightMode = highlightMode
                                )
                            } else {
                                FeatureTableItemValue.Text(
                                    content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                    planType = it.type,
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
                                    highlightMode = highlightMode
                                )
                            } else {
                                FeatureTableItemValue.Text(
                                    content = MR.plurals.groups_sharing_duration_type_days.localizedString(
                                        quantity = it.config.historyDaysLimit,
                                        it.config.historyDaysLimit
                                    ),
                                    planType = it.type,
                                    highlightMode = highlightMode
                                )
                            }
                        }
                    )
                )

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Section(
                            content = MR.strings.paywall_feature_personalSpace.localizedString(),
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
                                    FeatureTableItemValue.MoreInfo(
                                        title = it.config.adjustedPlanName,
                                        content = MR.strings.paywall_feature_history_days_minimum_info.localizedString(),
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                } else if (it.config.hasUnlimitedPersonalHistory) {
                                    FeatureTableItemValue.Text(
                                        content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                }  else {
                                    FeatureTableItemValue.Text(
                                        content = MR.plurals.groups_sharing_duration_type_days.localizedString(
                                            quantity = it.config.personalHistoryDaysLimit,
                                            it.config.personalHistoryDaysLimit
                                        ),
                                        planType = it.type,
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
                                FeatureTableItemValue.Count(
                                    content = it.config.personalDevicesLimit.toString(),
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
                            content = MR.strings.paywall_feature_places_personal.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                val highlightMode = if (it.isSelected) {
                                    FeatureTableItemHighlightMode.FULL
                                } else {
                                    FeatureTableItemHighlightMode.NONE
                                }
                                if (it.config.personalPlacesLimit >= 0) {
                                    FeatureTableItemValue.Count(
                                        content = it.config.personalPlacesLimit.toString(),
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                } else {
                                    FeatureTableItemValue.Text(
                                        content = MR.strings.paywall_feature_value_unlimited.localizedString(),
                                        planType = it.type,
                                        highlightMode = highlightMode
                                    )
                                }
                            }
                        )
                    )
                }

                if (!isGroupPlanOnly) {
                    add(
                        FeatureTableItemState.Section(
                            content = MR.strings.paywall_feature_section_others.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            topSpacing = 16.dp
                        )
                    )
                }

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_locate.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.type.isPremium) "✅" else "❌",
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
                                content = if (it.type.isSilverOrMore) "✅" else "❌",
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

                add(
                    FeatureTableItemState.Detail(
                        content = MR.strings.paywall_feature_notify.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.type.isSilverOrMore) "✅" else "❌",
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

                    add(
                        FeatureTableItemState.Detail(
                            content = MR.strings.paywall_feature_support_privacy.localizedString(),
                            visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                            values = plans.map {
                                FeatureTableItemValue.Emoji(
                                    content = if (it.type.isPremium) "✅" else "❌",
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
                features = values
            )
        }

        private fun createCollapsedSummary(plans: List<PlanInfoState>): List<FeatureTableItemState> {
            val list = buildList {
                add(
                    FeatureTableItemState.Summary(
                        title = MR.strings.paywall_feature_groups.localizedString(),
                        itemsPerRow = 3,
                        topSpacing = 0.dp,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_COLLAPSED,
                        values = plans.map {
                            FeatureTableItemValue.Summary(
                                features = buildList {
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = it.config.groupsLimit.toString(),
                                            content = MR.plurals.paywall_feature_summary_groups.localizedString(
                                                quantity = it.config.groupsLimit
                                            ),
                                            isHighlighted = false
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = it.config.membersLimit.toString(),
                                            content = MR.strings.paywall_feature_summary_members.localizedString(),
                                            isHighlighted = false
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = it.config.devicesLimit.toString(),
                                            content = MR.plurals.paywall_feature_summary_devices.localizedString(
                                                quantity = it.config.devicesLimit
                                            ),
                                            isHighlighted = false
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = if (it.config.placesLimit >= 0) {
                                                it.config.placesLimit.toString()
                                            } else {
                                                MR.strings.paywall_feature_value_unlimited.localizedString()
                                            },
                                            content = MR.strings.paywall_feature_summary_places.localizedString(),
                                            isHighlighted = it.config.placesLimit < 0
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = if (!it.config.isGroupHistoryEnabled) {
                                                "❌"
                                            } else if (it.config.hasUnlimitedGroupHistory) {
                                                MR.strings.paywall_feature_value_unlimited.localizedString()
                                            } else {
                                                MR.plurals.groups_sharing_duration_type_days.localizedString(
                                                    quantity = it.config.historyDaysLimit,
                                                    it.config.historyDaysLimit
                                                )
                                            },
                                            content = MR.strings.paywall_feature_summary_history.localizedString(),
                                            isHighlighted = it.config.hasUnlimitedGroupHistory
                                        )
                                    )
                                },
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
                    FeatureTableItemState.Summary(
                        title = MR.strings.paywall_feature_personalSpace.localizedString(),
                        itemsPerRow = 2,
                        topSpacing = 8.dp,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_COLLAPSED,
                        values = plans.map {
                            FeatureTableItemValue.Summary(
                                features = buildList {
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = it.config.personalDevicesLimit.toString(),
                                            content = MR.plurals.paywall_feature_summary_devices.localizedString(
                                                quantity = it.config.personalDevicesLimit
                                            ),
                                            isHighlighted = false
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = if (it.config.personalPlacesLimit >= 0) {
                                                it.config.personalPlacesLimit.toString()
                                            } else {
                                                MR.strings.paywall_feature_value_unlimited.localizedString()
                                            },
                                            content = MR.strings.paywall_feature_summary_places.localizedString(),
                                            isHighlighted = it.config.personalPlacesLimit < 0
                                        )
                                    )
                                    add(
                                        FeatureTableItemValueDataPoint(
                                            amount = if (!it.config.isPersonalHistoryEnabled) {
                                                "❌"
                                            } else if (it.config.hasUnlimitedPersonalHistory){
                                                MR.strings.paywall_feature_value_unlimited.localizedString()
                                            } else {
                                                MR.plurals.groups_sharing_duration_type_days.localizedString(
                                                    quantity = it.config.personalHistoryDaysLimit,
                                                    it.config.personalHistoryDaysLimit
                                                )
                                            },
                                            content = MR.strings.paywall_feature_summary_history.localizedString(),
                                            isHighlighted = it.config.hasUnlimitedPersonalHistory
                                        )
                                    )
                                },
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
                        content = MR.strings.paywall_feature_all_premium.localizedString(),
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_COLLAPSED,
                        isInSection = false,
                        values = plans.map {
                            FeatureTableItemValue.Emoji(
                                content = if (it.type.isPremium) "✅" else "❌",
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

            return list
        }
    }
}

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

    data class Summary(
        val title: String,
        val itemsPerRow: Int,
        val topSpacing: Dp,
        override val visibilityMode: FeatureTableItemVisibilityMode,
        override val values: List<FeatureTableItemValue.Summary>,
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
    val isPremium: Boolean get() = planType.isPremium

    data class Title(
        val content: String,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Count(
        val content: String,
        override val planType: PlanType,
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
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue

    data class Summary(
        val features: List<FeatureTableItemValueDataPoint>,
        override val planType: PlanType,
        override val highlightMode: FeatureTableItemHighlightMode,
    ) : FeatureTableItemValue
}

data class FeatureTableItemValueDataPoint(
    val amount: String,
    val content: String,
    val isHighlighted: Boolean
)

enum class FeatureTableItemHighlightMode {
    NONE, FULL, TOP, BOTTOM, SINGLE
}

enum class FeatureTableItemVisibilityMode {
    VISIBLE_ALWAYS, VISIBLE_EXPANDED, VISIBLE_COLLAPSED
}