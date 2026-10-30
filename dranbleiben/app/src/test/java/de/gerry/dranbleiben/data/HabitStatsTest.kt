package de.gerry.dranbleiben.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Gleiche Fälle wie auf dem Server (kopffrei-cloud/src/habits.js) */
class HabitStatsTest {

    private val today = LocalDate.of(2026, 10, 30) // Freitag
    private fun createdDaysAgo(n: Long) = today.minusDays(n).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun logs(habit: Habit, vararg entries: Pair<Long, Any>) = entries.associate { (ago, v) ->
        val d = today.minusDays(ago)
        d to when (v) {
            is Boolean -> HabitLog(habit.uid, d, done = v)
            else -> HabitLog(habit.uid, d, done = false, value = (v as Number).toDouble())
        }
    }

    @Test
    fun streakCountsUntilYesterdayWhileTodayIsOpen() {
        val h = Habit("h", "Meditieren", createdAt = createdDaysAgo(10))
        val stats = HabitStats.of(h, logs(h, 1L to true, 2L to true, 3L to true, 4L to false, 5L to true), today)
        assertEquals(3, stats.current)
        assertEquals(3, stats.best)
    }

    @Test
    fun streakIncludesToday() {
        val h = Habit("h", "Meditieren", createdAt = createdDaysAgo(10))
        assertEquals(3, HabitStats.of(h, logs(h, 0L to true, 1L to true, 2L to true), today).current)
    }

    @Test
    fun unplannedDaysDoNotBreakTheStreak() {
        // Mo, Mi, Fr – heute Freitag offen, Mi und Mo geschafft
        val h = Habit("h", "Training", days = setOf(1, 3, 5), createdAt = createdDaysAgo(10))
        val stats = HabitStats.of(h, logs(h, 2L to true, 4L to true), today)
        assertEquals(2, stats.current)
    }

    @Test
    fun measureNeedsTarget() {
        val min = Habit("h", "Lesen", type = HabitType.MEASURE, unit = "Min.", target = 20.0)
        assertEquals(true, min.isSuccess(HabitLog("h", today, false, 25.0)))
        assertEquals(false, min.isSuccess(HabitLog("h", today, false, 10.0)))
        val max = min.copy(targetMode = TargetMode.MAX)
        assertEquals(true, max.isSuccess(HabitLog("h", today, false, 10.0)))
        val free = min.copy(target = null)
        assertEquals(true, free.isSuccess(HabitLog("h", today, false, 81.4)))
        assertNull(free.isSuccess(null))
    }

    @Test
    fun rateUsesOnlyDaysSinceStart() {
        val h = Habit("h", "Meditieren", createdAt = createdDaysAgo(3))
        val stats = HabitStats.of(h, logs(h, 1L to true, 2L to true), today)
        assertEquals(4, stats.planned30)
        assertEquals(2, stats.done30)
        assertEquals(50, stats.rate30)
    }

    @Test
    fun formatsGermanNumbers() {
        assertEquals("81,4", 81.4.fmt())
        assertEquals("20", 20.0.fmt())
        assertEquals("7,5", 7.5.fmt())
        assertEquals(7.5, "7,5".toNumber())
    }
}
