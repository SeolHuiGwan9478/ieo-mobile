package com.ieo.mobile.ui.ledger

import java.io.File
import com.ieo.mobile.ui.component.QuietButton
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.ledger.LedgerDay
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)
private val BarOff = Color(0xFFD1D6DB)
private val DayFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

/** 수익 탭 (M10) */
@OptIn(ExperimentalMaterial3Api::class)
/** 파일을 앱 임시 폴더에 쓰고 공유 창을 연다 */
private fun shareLedgerFile(context: Context, file: ExportedLedger) {
    runCatching {
        val folder = File(context.cacheDir, "exports").apply { mkdirs() }
        val target = File(folder, file.fileName).apply { writeBytes(file.bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", target)
        val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "보낼 곳을 골라 주세요"))
    }
}

@Composable
private fun ExportDialog(viewModel: LedgerViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeExport,
        title = { Text("어느 기간을 내보낼까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportPeriod.entries.forEach { period -> TonalButton(period.label, { viewModel.export(period) }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::closeExport) { Text("취소", color = TextMid) } },
    )
}

@Composable
fun LedgerScreen(viewModel: LedgerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ledger = state.ledger
    val days = ledger.days()
    val context = LocalContext.current

    LaunchedEffect(state.exported) {
        state.exported?.let { file ->
            shareLedgerFile(context, file)
            viewModel.exportConsumed()
        }
    }

    Column(Modifier.fillMaxSize().background(CanvasGray)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("수익", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong, modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::previousMonth, enabled = state.canGoBack) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "이전 달", tint = if (state.canGoBack) TextStrong else BarOff) }
            Text(monthLabel(state.month, state.currentMonth.year), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            IconButton(onClick = viewModel::nextMonth, enabled = state.canGoForward) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "다음 달", tint = if (state.canGoForward) TextStrong else BarOff) }
        }
        PullToRefreshBox(isRefreshing = state.isLoading && state.entries.isNotEmpty(), onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.notice?.let { NoticeBar(it, viewModel::dismissNotice) }
                if (state.loadFailed) state.error?.let { ErrorNotice(it, viewModel::refresh) }
                if (state.isLoading && state.entries.isEmpty()) {
                    SkeletonCards(3)
                } else {
                    SummaryCard(state, viewModel)
                    if (state.owedTotal > 0) OwedCard(state, viewModel)
                    if (state.expenseByCategory.isNotEmpty()) ExpenseCard(state.expenseByCategory, ledger.expenseTotal)
                    if (days.isEmpty()) {
                        EmptyState("이 달 기록이 없어요", "운행을 마치면 장부에 저절로 적혀요")
                    }
                    days.forEach { DaySection(it, viewModel) }
                    state.error?.takeIf { !state.loadFailed }?.let { ErrorNotice(it) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        QuietButton(if (state.isBusy) "장부 받는 중…" else "세금 신고용 장부 내보내기", viewModel::openExport)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton("운행 추가", viewModel::openManual, modifier = Modifier.weight(1f))
            PrimaryButton("경비 추가", viewModel::openExpense, modifier = Modifier.weight(1.4f))
        }
    }
    if (state.choosingExport) ExportDialog(viewModel)
}

@Composable
private fun SummaryCard(state: LedgerState, viewModel: LedgerViewModel) {
    val ledger = state.ledger
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("${monthLabel(state.month, state.currentMonth.year)} 순수익", fontSize = 16.sp, color = TextMid)
        Text(formatWon(ledger.netProfit), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
        Text("매출 ${formatWon(ledger.revenue)} − 경비 ${formatWon(ledger.expenseTotal)}", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.height(14.dp))
        MonthBars(state, viewModel)
    }
}

/** 최근 6개월 순수익. 누르면 그 달로 간다. 고른 달만 금액을 쓴다 */
@Composable
private fun MonthBars(state: LedgerState, viewModel: LedgerViewModel) {
    val points = state.history
    val max = points.maxOf { it.net }.coerceAtLeast(1)
    val summary = points.joinToString(", ") { "${it.month.monthValue}월 ${shortWon(it.net)}" }
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "최근 6개월 순수익: $summary" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val selected = point.month == state.month
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable { viewModel.selectMonth(point.month) },
            ) {
                Text(if (selected) shortWon(point.net) else "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Box(Modifier.height(72.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.width(22.dp)
                            .fillMaxHeight((point.net.toFloat() / max).coerceIn(0.04f, 1f))
                            .background(if (selected) Orange600 else BarOff, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    )
                }
                Text("${point.month.monthValue}월", fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) TextStrong else TextMid, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun OwedCard(state: LedgerState, viewModel: LedgerViewModel) {
    val owed = state.receivables ?: return
    Row(
        Modifier.fillMaxWidth().background(White, CardShape).clickable(onClick = viewModel::openReceivables).heightIn(min = 72.dp).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("못 받은 돈 ${formatWon(state.owedTotal)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
            if (owed.overdueFareTotal > 0) Text("예정일 지남 ${formatWon(owed.overdueFareTotal)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IeoStatus.colors.warning)
        }
        Text("›", fontSize = 22.sp, color = TextSub)
    }
}

/** 분류별 경비. 범례 없이 줄마다 이름과 금액을 쓴다 */
@Composable
private fun ExpenseCard(items: List<Pair<ExpenseCategory, Long>>, total: Long) {
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AmountLine("경비", formatWon(total), strong = true)
        items.forEach { (category, amount) ->
            Column {
                AmountLine(category.label, formatWon(amount))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(CanvasGray, RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxWidth((amount.toFloat() / total).coerceIn(0.02f, 1f)).height(6.dp).background(Orange600, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

@Composable
private fun DaySection(day: LedgerDay, viewModel: LedgerViewModel) {
    Text(day.date.format(DayFormat), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(vertical = 4.dp)) {
        day.entries.forEach { EntryRow(it) { viewModel.openEntry(it) } }
        day.expenses.forEach { ExpenseRow(it) }
    }
}

@Composable
fun EntryRow(entry: LedgerEntry, onClick: () -> Unit) {
    val (text, color) = paidTag(entry)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.description, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
            Text(formatWon(entry.total), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTag(text, color)
            Text(listOfNotNull(entry.paymentType.label, entry.brokerName).joinToString(" · "), fontSize = 15.sp, color = TextSub)
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(listOfNotNull(expense.category.label, expense.memo).joinToString(" · "), fontSize = 16.sp, color = TextMid, modifier = Modifier.weight(1f))
        Text("−${formatWon(expense.amount)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMid)
    }
}
