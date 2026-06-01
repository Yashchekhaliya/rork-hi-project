package com.rork.workpulse.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Generates CSV reports for payroll and attendance and shares them via Android's
 * share sheet. Excel / Google Sheets open CSV files natively.
 *
 * Three report types:
 * - Monthly payroll: one row per employee for a given month
 * - Yearly payroll: one row per employee with Jan..Dec + total columns
 * - Full attendance log: every check-in/out entry for a month
 */
object CsvExporter {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** Write a monthly payroll CSV, then fire a share intent. */
    fun exportMonthlyPayroll(context: Context, month: Int, year: Int) {
        val salaries = WorkPulseRepository.allSalaries(month, year)
        val header = "Employee,Base Salary,Days Worked,Paid Leave,Unpaid Leave,Absences,Per Day,Gross,Deductions,Net\n"
        val rows = salaries.joinToString("") { s ->
            "${csv(s.employeeName)},${s.baseSalary},${Format.fracDays(s.daysWorked)},${s.paidLeaveDays},${s.unpaidLeaveDays},${Format.fracDays(s.absentDays)},${s.perDayRate},${s.grossEarned},${s.deductions},${s.netPayable}\n"
        }
        val csv = header + rows
        val totalRow = "TOTAL,,,,,,,," +
            salaries.sumOf { it.grossEarned } + "," +
            salaries.sumOf { it.deductions } + "," +
            salaries.sumOf { it.netPayable } + "\n"
        val filename = "Payroll_${Format.monthName(month)}_$year.csv"
        share(context, filename, csv + totalRow)
    }

    /** Write a yearly summary CSV with Jan..Dec columns + total. */
    fun exportYearlySummary(context: Context, year: Int) {
        val summaries = WorkPulseRepository.yearlySummaries(year)
        val months = Format.shortMonthNames
        val header = "Employee,Base Salary," + months.joinToString(",") + ",Total\n"
        val rows = summaries.joinToString("") { ys ->
            "${csv(ys.employeeName)},${ys.baseSalary}," +
                ys.monthlyNet.joinToString(",") { it.toString() } +
                ",${ys.totalNet}\n"
        }
        val filename = "YearlySummary_$year.csv"
        share(context, filename, header + rows)
    }

    /** Full attendance log for all employees in a given month. */
    fun exportAttendanceLog(context: Context, month: Int, year: Int) {
        val logs = WorkPulseRepository.allLogsForMonth(month, year)
        val employees = WorkPulseRepository.employees.value.associateBy { it.id }
        val header = "Date,Employee,Check In,Check Out,Duration (h),Verified,Distance (m)\n"
        val rows = logs.joinToString("") { log ->
            val emp = employees[log.employeeId]
            val outStr = log.checkOutMillis?.let { Format.time(it) } ?: "active"
            val durH = if (log.checkOutMillis != null)
                "%.2f".format(Format.hoursDecimal(log.checkOutMillis!! - log.checkInMillis))
            else ""
            val verified = if (log.verified) "Yes" else "No"
            "${Format.isoDate(log.checkInMillis)},${csv(emp?.name ?: log.employeeId)},${Format.time(log.checkInMillis)},$outStr,$durH,$verified,${log.distanceFromSite.toInt()}\n"
        }
        val filename = "Attendance_${Format.monthName(month)}_$year.csv"
        share(context, filename, header + rows)
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun share(context: Context, filename: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$AUTHORITY_SUFFIX",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $filename"))
    }
}
