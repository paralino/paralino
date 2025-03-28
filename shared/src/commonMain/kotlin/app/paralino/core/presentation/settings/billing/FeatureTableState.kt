package app.paralino.core.presentation.settings.billing

import app.paralino.core.MR
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.localizedString

data class FeatureTableState(
    val canExpand: Boolean,
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

            return constructFeaturesTable(plans = plans)
        }

        fun constructFeaturesTable(
            plans: List<PlanInfoState>
        ): FeatureTableState {
            val values = buildList {
                add(
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_table_header.localizedString(),
                        isTitle = true,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = it.name,
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

                add(
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_max_privacy.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = "✅",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_groups.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = it.config.groupsLimit.toString(),
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_members.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = it.config.membersLimit.toString(),
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_devices.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = it.config.devicesLimit.toString(),
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_places.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = it.config.placesLimit.toString(),
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_battery.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = "✅",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_realtime_update.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isPremium) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_locate.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isPremium) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_performance.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isSilverOrMore) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_multi_device.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.config.devicesLimit > 1) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_notify.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_EXPANDED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isSilverOrMore) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_all_premium.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_COLLAPSED,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isPremium) "✅" else "❌",
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
                    FeatureTableItemState(
                        name = MR.strings.paywall_feature_support_privacy.localizedString(),
                        isTitle = false,
                        visibilityMode = FeatureTableItemVisibilityMode.VISIBLE_ALWAYS,
                        values = plans.map {
                            FeatureTableItemValue(
                                name = if (it.type.isPremium) "✅" else "❌",
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

            return FeatureTableState(canExpand = false, features = values)
        }
    }
}

data class FeatureTableItemState(
    val name: String,
    val isTitle: Boolean,
    val visibilityMode: FeatureTableItemVisibilityMode,
    val values: List<FeatureTableItemValue>
)

data class FeatureTableItemValue(
    val name: String,
    val planType: PlanType,
    val highlightMode: FeatureTableItemHighlightMode,
) {
    val isPremium: Boolean = planType.isPremium
}

enum class FeatureTableItemHighlightMode {
    NONE, FULL, TOP, BOTTOM
}

enum class FeatureTableItemVisibilityMode {
    VISIBLE_ALWAYS, VISIBLE_EXPANDED, VISIBLE_COLLAPSED
}