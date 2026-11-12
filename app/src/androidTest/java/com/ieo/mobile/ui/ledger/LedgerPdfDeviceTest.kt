package com.ieo.mobile.ui.ledger

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerReport
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.StopType
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기기에서 실제 PDF를 그려 앱 폴더(files/ledger.pdf)에 남긴다. 두 쪽을 넘기는 긴 장부로 장 넘김도 본다.
 *   ./gradlew connectedDebugAndroidTest
 *   adb shell run-as com.ieo.mobile cat files/ledger.pdf > ledger.pdf
 */
@RunWith(AndroidJUnit4::class)
class LedgerPdfDeviceTest {

    @Test
    fun 장부를_PDF로_그린다() {
        val nov = LocalDate.of(2026, 11, 1)
        val routes = listOf("평택 포승 → 대구 성서", "이천 냉동창고 → 부산 신항 CY", "김포 한강 물류 → 평택 물류센터", "대전 산업단지 → 김포 한강 물류")
        val brokers = listOf("한빛물류", "대한운송", "○○물류", null)
        val entries = (1..36).map { i ->
            val day = nov.withDayOfMonth((i - 1) % 28 + 1)
            LedgerEntry(
                i.toLong(), true, day, routes[i % routes.size], 300_000L + (i % 5) * 40_000, PaymentType.entries[i % 3], brokers[i % brokers.size],
                null, null, day.plusDays(30), paid = i % 3 == 0, overdue = i % 7 == 0,
                extraIncomes = if (i % 4 == 0) listOf(ExtraIncome(i.toLong(), StopType.PICKUP, "평택", 10_000, false)) else emptyList(),
            )
        }
        val expenses = (1..12).map { i ->
            Expense(i.toLong(), nov.withDayOfMonth(i * 2), ExpenseCategory.entries[i % 7], 9_000L + i * 13_000, if (i % 2 == 0) "경부선 왕복" else null)
        }
        val report = LedgerReport(nov, nov.withDayOfMonth(30), entries, expenses, LocalDate.of(2026, 11, 12), driverName = "김기사")
        val bytes = LedgerPdf.render(report)
        assertEquals("%PDF", String(bytes.copyOfRange(0, 4)))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "ledger.pdf").writeBytes(bytes)
    }
}
