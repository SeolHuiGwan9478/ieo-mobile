package com.ieo.mobile.ui.ledger

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerReport
import com.ieo.mobile.domain.trip.PaymentType
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * 장부를 A4 PDF로 그린다 (LEDG-033). 휴대폰 안에서 만들어 공유 창으로 보낸다. 서버를 거치지 않는다.
 * 첫 장에 기간·합계, 이어서 운행 표와 경비 표. 표가 길면 다음 장으로 넘기고 머리줄을 다시 그린다.
 * 금액은 원 단위 숫자(450,000)로 적어 세무 신고 때 옮겨 적기 쉽게 한다
 */
object LedgerPdf {

    // A4 (pt)
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val ROW_H = 20f
    private const val BOTTOM = PAGE_H - 52f

    private const val INK = 0xFF191F28.toInt()
    private const val SUB = 0xFF6B7684.toInt()
    private const val LINE = 0xFFE5E8EB.toInt()
    private const val ACCENT = 0xFFF25C27.toInt()
    private const val WARN = 0xFFD22030.toInt()
    private const val GOOD = 0xFF1E8A4C.toInt()
    private const val SOFT = 0xFFF7F8FA.toInt()

    private val number = NumberFormat.getNumberInstance(Locale.KOREA)

    private fun won(amount: Long) = number.format(amount)

    private fun text(size: Float, color: Int = INK, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textAlign = align
    }

    private fun fill(color: Int) = Paint().apply { this.color = color; style = Paint.Style.FILL }

    private fun stroke(color: Int, width: Float = 0.6f) = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }

    /** 칸보다 길면 뒤를 줄이고 "…" */
    private fun fit(value: String, width: Float, paint: Paint): String {
        if (paint.measureText(value) <= width) return value
        var end = value.length
        while (end > 0 && paint.measureText(value.substring(0, end) + "…") > width) end--
        return value.substring(0, end) + "…"
    }

    private class Column(val title: String, val width: Float, val right: Boolean = false)

    private val tripColumns = listOf(
        Column("날짜", 46f), Column("구간", 168f), Column("주선사", 80f), Column("결제", 38f),
        Column("운임", 62f, right = true), Column("대기비", 54f, right = true), Column("지급", 75f),
    )
    private val expenseColumns = listOf(
        Column("날짜", 46f), Column("분류", 60f), Column("메모", 325f), Column("금액", 92f, right = true),
    )

    fun render(report: LedgerReport): ByteArray {
        val document = PdfDocument()
        val writer = Writer(document, report)
        writer.header()
        writer.summary()
        if (report.isEmpty) {
            writer.note("이 기간에 기록한 운행과 경비가 없어요.")
        } else {
            if (report.entries.isNotEmpty()) writer.table("운행 ${report.tripCount}건", tripColumns, report.entries.map(::tripCells), report.entries.map(::tripColor))
            if (report.expenses.isNotEmpty()) {
                writer.table(
                    "경비 ${report.expenses.size}건",
                    expenseColumns,
                    report.expenses.map { listOf(day(it.spentOn), it.category.label, it.memo.orEmpty(), won(it.amount)) },
                    report.expenses.map { null },
                )
                writer.categories()
            }
        }
        writer.finish()
        return ByteArrayOutputStream().use { out ->
            document.writeTo(out)
            document.close()
            out.toByteArray()
        }
    }

    private fun day(date: java.time.LocalDate) = "${date.monthValue}.${date.dayOfMonth}"

    private fun payment(type: PaymentType) = when (type) {
        PaymentType.PREPAID -> "선불"
        PaymentType.COLLECT -> "착불"
        PaymentType.RECEIPT -> "인수증"
    }

    private fun tripCells(entry: LedgerEntry): List<String> {
        val waitFee = entry.extraIncomes.sumOf { it.amount }
        val pay = when {
            entry.paid -> "받음"
            entry.expectedPaymentDate == null -> "-"
            entry.overdue -> "${day(entry.expectedPaymentDate)} 지남"
            else -> "${day(entry.expectedPaymentDate)} 예정"
        }
        return listOf(
            day(entry.workDate), entry.description, entry.brokerName ?: "직접 등록", payment(entry.paymentType),
            won(entry.fare), if (waitFee > 0) won(waitFee) else "-", pay,
        )
    }

    /** 지급 칸 색: 받음은 초록, 지남은 빨강 */
    private fun tripColor(entry: LedgerEntry): Int? = when {
        entry.paid -> GOOD
        entry.overdue -> WARN
        else -> null
    }

    /** 장을 넘기며 위에서 아래로 쓴다 */
    private class Writer(private val document: PdfDocument, private val report: LedgerReport) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        var y = 0f

        init {
            newPage()
        }

        private fun newPage() {
            page?.let { finishPage(it) }
            pageNumber++
            val created = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            page = created
            canvas = created.canvas
            y = MARGIN
        }

        private fun finishPage(current: PdfDocument.Page) {
            val footer = PAGE_H - 28f
            current.canvas.drawLine(MARGIN, footer - 12f, PAGE_W - MARGIN, footer - 12f, stroke(LINE))
            current.canvas.drawText("이에오 장부 · ${report.periodLabel}", MARGIN, footer, text(8f, SUB))
            current.canvas.drawText("${pageNumber}쪽", PAGE_W - MARGIN, footer, text(8f, SUB, align = Paint.Align.RIGHT))
            document.finishPage(current)
        }

        fun finish() {
            page?.let { finishPage(it) }
            page = null
        }

        private fun ensure(height: Float): Boolean {
            if (y + height <= BOTTOM) return false
            newPage()
            return true
        }

        fun header() {
            canvas.drawRect(MARGIN, y, MARGIN + 28f, y + 4f, fill(ACCENT))
            y += 30f
            canvas.drawText("장부", MARGIN, y, text(22f, bold = true))
            canvas.drawText(report.periodLabel, MARGIN + 56f, y, text(16f, SUB))
            val who = listOfNotNull(report.driverName, "만든 날 ${LedgerReport.dotted(report.madeOn)}").joinToString(" · ")
            canvas.drawText(who, PAGE_W - MARGIN, y, text(9f, SUB, align = Paint.Align.RIGHT))
            y += 22f
        }

        fun summary() {
            val boxes = listOf(
                "운임" to won(report.fareTotal),
                "대기비" to won(report.waitFeeTotal),
                "경비" to won(report.expenseTotal),
                "실수익" to won(report.netProfit),
            )
            val gap = 8f
            val width = (PAGE_W - MARGIN * 2 - gap * (boxes.size - 1)) / boxes.size
            boxes.forEachIndexed { index, (label, value) ->
                val left = MARGIN + index * (width + gap)
                canvas.drawRoundRect(left, y, left + width, y + 52f, 6f, 6f, fill(SOFT))
                canvas.drawText(label, left + 10f, y + 18f, text(8.5f, SUB))
                val last = index == boxes.lastIndex
                canvas.drawText("$value 원", left + 10f, y + 40f, text(14f, if (last) ACCENT else INK, bold = true))
            }
            y += 70f
            val owed = buildString {
                append("운행 ${report.tripCount}건")
                if (report.unpaidFare > 0 || report.unpaidWaitFee > 0) {
                    append(" · 아직 못 받은 돈: 운임 ${won(report.unpaidFare)}원")
                    if (report.unpaidWaitFee > 0) append(", 대기비 ${won(report.unpaidWaitFee)}원")
                }
            }
            canvas.drawText(owed, MARGIN, y, text(9.5f, SUB))
            y += 26f
        }

        fun note(message: String) {
            canvas.drawText(message, MARGIN, y + 12f, text(11f, SUB))
            y += 30f
        }

        fun table(title: String, columns: List<Column>, rows: List<List<String>>, lastColumnColors: List<Int?>) {
            ensure(ROW_H * 3)
            canvas.drawText(title, MARGIN, y + 12f, text(12f, bold = true))
            y += 22f
            columnHeader(columns)
            rows.forEachIndexed { index, cells ->
                if (ensure(ROW_H)) {
                    canvas.drawText("$title (이어서)", MARGIN, y + 12f, text(10f, SUB, bold = true))
                    y += 20f
                    columnHeader(columns)
                }
                var x = MARGIN
                columns.forEachIndexed { col, column ->
                    val color = if (col == columns.lastIndex) lastColumnColors[index] else null
                    val paint = text(9f, color ?: INK, bold = color != null, align = if (column.right) Paint.Align.RIGHT else Paint.Align.LEFT)
                    val value = fit(cells[col], column.width - 8f, paint)
                    val tx = if (column.right) x + column.width - 4f else x + 4f
                    canvas.drawText(value, tx, y + 13.5f, paint)
                    x += column.width
                }
                y += ROW_H
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, stroke(LINE))
            }
            y += 18f
        }

        private fun columnHeader(columns: List<Column>) {
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + ROW_H, fill(SOFT))
            var x = MARGIN
            columns.forEach { column ->
                val paint = text(8.5f, SUB, bold = true, align = if (column.right) Paint.Align.RIGHT else Paint.Align.LEFT)
                canvas.drawText(column.title, if (column.right) x + column.width - 4f else x + 4f, y + 13.5f, paint)
                x += column.width
            }
            y += ROW_H
        }

        fun categories() {
            val lines = report.expenseByCategory
            ensure(ROW_H * (lines.size + 2))
            canvas.drawText("경비 분류별", MARGIN, y + 12f, text(11f, bold = true))
            y += 22f
            lines.forEach { (category, amount) ->
                canvas.drawText(category.label, MARGIN + 4f, y + 12f, text(9.5f, SUB))
                canvas.drawText("${won(amount)}원", MARGIN + 160f, y + 12f, text(9.5f, align = Paint.Align.RIGHT))
                y += 17f
            }
            y += 10f
        }
    }
}
