package com.rork.workpulse.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Lightweight date/time formatting helpers. */
object Format {
    private val time = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayMonth = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val shortDate = SimpleDateFormat("MMM d", Locale.getDefault())
    private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun time(millis: Long): String = time.format(Date(millis))
    fun dayMonth(millis: Long): String = dayMonth.format(Date(millis))
    fun shortDate(millis: Long): String = shortDate.format(Date(millis))
    fun isoDate(millis: Long): String = isoDate.format(Date(millis))

    /** Formats a duration in ms as "8h 12m" (or "0h 04m" live). */
    fun duration(millis: Long): String {
        val totalMin = (millis / 60000).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return "${h}h ${m.toString().padStart(2, '0')}m"
    }

    fun hoursDecimal(millis: Long): Double = (millis / 3_600_000.0)

    /** Formats fractional days as a clean string: "15.5" or "15" if whole. */
    fun fracDays(days: Double): String {
        val rounded = (days * 10.0).roundToInt() / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else "%.1f".format(rounded)
    }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    val shortMonthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun monthName(month: Int): String = monthNames.getOrElse(month) { "Unknown" }
    fun shortMonthName(month: Int): String = shortMonthNames.getOrElse(month) { "???" }

    /** "June 2026" style header. */
    fun monthYear(month: Int, year: Int): String = "${monthName(month)} $year"

    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH)
    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
