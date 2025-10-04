package app.paralino.core.presentation.history

import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class LocationHistoryDateMapper(
    private val userRepo: UserRepository,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val remoteConfig: RemoteConfigRepository,
    private val valueFormatter: IValueFormatter,
) {
    suspend fun createDateSelector(
        groupId: String,
        selectedDate: LocalDate,
        dateRangeDays: Int,
        daysInLoading: List<LocalDate>,
    ): LocationHistoryDateSelectorState {
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId
        val isGroupOwner = if (isPersonalHistory) {
            true
        } else {
            userGroupRepo.allGroupsState.value.data?.firstOrNull { it.id == groupId }
                ?.group?.isCurrentUserOwner ?: false
        }

        val planConfig = if (isPersonalHistory) {
            userRepo.userPlans.value.highestValidPlanConfig
        } else {
            userGroupRepo.allGroupsState.value.data?.firstOrNull { it.id == groupId }
                ?.group?.planConfig(remoteConfig.getConfigOnce())
                ?: PlanConfig.default
        }

        val highestAppPlan = remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order } ?: PlanConfig.default
        val upgradeAvailable = highestAppPlan.planTypeEnum.order > planConfig.planTypeEnum.order
        val isLimitedHistoryPlan = if (isPersonalHistory) {
            planConfig.personalHistoryDaysLimit == 0
        } else {
            planConfig.historyDaysLimit == 0
        }

        val earliestDate = if (isPersonalHistory) {
            if (planConfig.personalHistoryDaysLimit >= 0) {
                today.minus(DatePeriod(days = planConfig.personalHistoryDaysLimit))
            } else {
                null
            }
        } else {
            if (planConfig.historyDaysLimit >= 0) {
                today.minus(DatePeriod(days = planConfig.historyDaysLimit))
            } else {
                null
            }
        }
        val couldLoadMore = earliestDate == null

        val startAction = if (couldLoadMore) {
            LocationHistoryDateSelectionAction.LOAD_MODE
        } else if (upgradeAvailable) {
            if (isGroupOwner) {
                LocationHistoryDateSelectionAction.UPGRADE
            } else {
                LocationHistoryDateSelectionAction.ASK_FOR_UPGRADE
            }
        } else {
            LocationHistoryDateSelectionAction.END
        }

        return LocationHistoryDateSelectorState(
            maxDateRangeDays = LocationHistoryScreenState.maxDateRangeDays,
            isLimitedHistoryPlanMode = isLimitedHistoryPlan,
            days = if (isLimitedHistoryPlan) {
                generateDates(
                    dateRangeDays = 1,
                    startDate = today,
                    earliestDate = today.minus(DatePeriod(days = 60))
                ).map { it.copy(isSelected = false) }
            } else {
                generateDates(
                    dateRangeDays = dateRangeDays,
                    startDate = selectedDate,
                    earliestDate = earliestDate
                )
            },
            daysInLoading = daysInLoading,
            dateRangeDays = dateRangeDays,
            startAction = startAction,
            earliestDate = earliestDate
        )
    }

    fun generateDates(
        dateRangeDays: Int,
        startDate: LocalDate,
        earliestDate: LocalDate?,
    ): List<LocationHistoryDayState> {
        val dates = mutableListOf<LocationHistoryDayState>()
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Calculate which days are selected (dateRangeDays going back from startDate)
        val selectedDates = mutableSetOf<LocalDate>()
        repeat(dateRangeDays) {
            startDate.minus(DatePeriod(days = it)).takeIf {
                if (earliestDate != null) {
                    it >= earliestDate
                } else {
                    true
                }
            }?.let {
                selectedDates.add(it)
            }
        }

        // Sort selected dates to calculate positions correctly
        val sortedSelectedDates = selectedDates.sorted()

        // Determine the earliest date to generate
        // Minimum: previous month start, but could be earlier if startDate is older
        val earliestDate = if (earliestDate == null) {
            val previousMonth = today.minus(DatePeriod(months = 1))
            val previousMonthStart = LocalDate(previousMonth.year, previousMonth.month, 1)
            minOf(startDate, previousMonthStart)
        } else {
            earliestDate
        }

        // Generate all days from earliest date to today
        var currentDate = earliestDate
        while (currentDate <= today) {
            dates.add(
                LocationHistoryDayState(
                    isSelected = selectedDates.contains(currentDate),
                    isLoading = false,
                    date = currentDate,
                    formattedMonth = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "MMM"
                    ),
                    formattedDay = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "EEE"
                    ),
                    isWeekend = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY,
                    position = calculateSelectedDayPosition(currentDate, sortedSelectedDates)
                )
            )
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        return dates
    }

    fun createDatesForPreviousMonth(
        selectedDate: LocalDate,
        dateRangeDays: Int?,
        currentDays: List<LocationHistoryDayState>
    ): List<LocationHistoryDayState>? {
        if (currentDays.isEmpty()) return null

        // Find the oldest date in current days
        val oldestDate = currentDays.minByOrNull { it.date }?.date ?: return null

        // Get the previous month
        val previousMonth = oldestDate.minus(DatePeriod(months = 1))
        val previousMonthStart = LocalDate(previousMonth.year, previousMonth.month, 1)
        val previousMonthEnd = LocalDate(oldestDate.year, oldestDate.month, 1)
            .minus(DatePeriod(days = 1))

        // Calculate which days are selected based on current selection
        val selectedDates = mutableSetOf<LocalDate>()
        repeat(dateRangeDays ?: 0) {
            selectedDates.add(selectedDate.minus(DatePeriod(days = it)))
        }
        val sortedSelectedDates = selectedDates.sorted()

        // Generate all days for the previous month
        val newDays = mutableListOf<LocationHistoryDayState>()
        var currentDate = previousMonthStart
        while (currentDate <= previousMonthEnd) {
            newDays.add(
                LocationHistoryDayState(
                    isSelected = selectedDates.contains(currentDate),
                    isLoading = false,
                    date = currentDate,
                    formattedMonth = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "MMM"
                    ),
                    formattedDay = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "EEE"
                    ),
                    isWeekend = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY,
                    position = calculateSelectedDayPosition(
                        date = currentDate,
                        sortedSelectedDates = sortedSelectedDates
                    )
                )
            )
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        // Prepend new days to existing days and update state
        val updatedDays = newDays + currentDays
        return updatedDays
    }

    fun calculateSelectedDayPosition(
        date: LocalDate,
        sortedSelectedDates: List<LocalDate>
    ): LocationHistorySelectedDayPosition {
        if (!sortedSelectedDates.contains(date)) {
            return LocationHistorySelectedDayPosition.SOLO
        }

        // If only one day is selected, it's SOLO
        if (sortedSelectedDates.size == 1) {
            return LocationHistorySelectedDayPosition.SOLO
        }

        val index = sortedSelectedDates.indexOf(date)

        return when {
            index == 0 -> LocationHistorySelectedDayPosition.START
            index == sortedSelectedDates.size - 1 -> LocationHistorySelectedDayPosition.END
            else -> LocationHistorySelectedDayPosition.CENTER
        }
    }
}