package com.ieo.mobile.ui.ledger

import com.ieo.mobile.ui.component.ruled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.Heading
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 수익 탭 위에 열리는 화면: 입력, 상세, 못 받은 돈 */
@Composable
fun LedgerOverlay(viewModel: LedgerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = viewModel::back)
    val form = state.form
    val entry = state.selected
    DeleteDialog(state.deleting, viewModel)
    when {
        form != null -> FormScreen(form, state, viewModel)
        entry != null -> EntryDetail(entry, state, viewModel)
        else -> ReceivablesScreen(state, viewModel)
    }
}

@Composable
private fun BackBar(title: String, viewModel: LedgerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        IconButton(onClick = viewModel::back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}

@Composable
private fun EntryDetail(entry: LedgerEntry, state: LedgerState, viewModel: LedgerViewModel) {
    val (text, color) = paidTag(entry)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        BackBar("장부 상세", viewModel)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            state.notice?.let { NoticeBar(it, viewModel::dismissNotice) }
            Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTag(text, color)
                Text(entry.description, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountLine("운임", formatWon(entry.fare), strong = true)
                AmountLine("결제", entry.paymentType.label)
                entry.brokerName?.let { AmountLine("주선사", it) }
                AmountLine("운행일", "${entry.workDate.monthValue}월 ${entry.workDate.dayOfMonth}일")
                entry.expectedPaymentDate?.let { AmountLine("지급 예정일", "${it.monthValue}월 ${it.dayOfMonth}일") }
                entry.distanceKm?.let { AmountLine("거리", "${it}km") }
                entry.waitMinutes?.let { AmountLine("대기", "${it}분") }
                QuietButton("운임·거리 수정", viewModel::openEditFare)
                // 직접 추가한 줄만 지운다. 운행이 끝나 생긴 줄은 운행 기록과 짝이라 지우지 않는다
                if (!entry.auto) QuietButton("이 운행 지우기", { viewModel.askDelete(entry) })
            }
            if (entry.extraIncomes.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().ruled().padding(vertical = 14.dp)) {
                    Text("대기비", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    entry.extraIncomes.forEach { ExtraRow(it, state.isBusy, viewModel) }
                }
            }
            state.error?.let { ErrorNotice(it) }
        }
        Column(Modifier.background(White).padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (entry.paid) {
                TonalButton("받기 전으로", { viewModel.setPaid(entry, false) }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton("돈 받았어요", { viewModel.setPaid(entry, true) }, busy = state.isBusy)
            }
        }
    }
}

@Composable
private fun ExtraRow(extra: ExtraIncome, busy: Boolean, viewModel: LedgerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(extra.siteName, fontSize = 16.sp, color = TextMid)
            Text(formatWon(extra.amount), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        QuietButton(if (extra.paid) "받음 취소" else "받았어요", { if (!busy) viewModel.setExtraPaid(extra, !extra.paid) })
    }
}

/** 못 받은 돈. 지급일이 지난 것부터 */
@Composable
private fun ReceivablesScreen(state: LedgerState, viewModel: LedgerViewModel) {
    val owed = state.receivables
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        BackBar("못 받은 돈", viewModel)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            state.notice?.let { NoticeBar(it, viewModel::dismissNotice) }
            state.error?.let { ErrorNotice(it) }
            Heading(formatWon(state.owedTotal))
            if (owed != null && owed.waitFeeTotal > 0) Text("운임 ${formatWon(owed.fareTotal)} · 대기비 ${formatWon(owed.waitFeeTotal)}", fontSize = 16.sp, color = TextMid)
            val entries = owed?.entries.orEmpty().sortedWith(compareByDescending<LedgerEntry> { it.overdue }.thenBy { it.expectedPaymentDate })
            if (entries.isEmpty()) EmptyState("못 받은 돈이 없어요", "운임을 모두 받으셨어요")
            entries.forEach { entry ->
                val (text, color) = paidTag(entry)
                Column(Modifier.fillMaxWidth().ruled().padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.brokerName ?: "주선사 미상", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text(entry.description, fontSize = 16.sp, color = TextMid)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusTag(text, color)
                        Text(formatWon(entry.fare), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
                        QuietButton("받았어요", { if (!state.isBusy) viewModel.setPaid(entry, true) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormScreen(form: LedgerForm, state: LedgerState, viewModel: LedgerViewModel) {
    val title = when (form.kind) {
        FormKind.EXPENSE -> "얼마 쓰셨어요?"
        FormKind.MANUAL -> "어떤 운행이었어요?"
        FormKind.EDIT_FARE -> "장부를 수정할게요"
    }
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        BackBar("", viewModel)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Heading(title)
            Spacer(Modifier.height(20.dp))
            if (form.kind == FormKind.MANUAL) {
                FormField(form.description, { value -> viewModel.editForm { it.copy(description = value.take(60)) } }, "구간 (평택 → 대구)")
                Spacer(Modifier.height(14.dp))
            }
            FormField(form.amount, viewModel::onAmount, "금액 (원)", KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1L, 5L, 10L).forEach { BrandChip("+${it}만", false) { viewModel.addAmount(it * 10_000) } }
            }
            if (form.amountValue > 0) Text(formatWon(form.amountValue), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(20.dp))
            if (form.kind == FormKind.EXPENSE) {
                Label("무엇에 쓰셨어요?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.forEach { category -> BrandChip(category.label, form.category == category) { viewModel.editForm { it.copy(category = category) } } }
                }
                Spacer(Modifier.height(18.dp))
                FormField(form.memo, { value -> viewModel.editForm { it.copy(memo = value.take(100)) } }, "메모 (없으면 비워 두세요)")
            } else {
                Label("결제")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentType.entries.forEach { type -> BrandChip(type.label, form.paymentType == type) { viewModel.editForm { it.copy(paymentType = type) } } }
                }
            }
            if (form.kind == FormKind.MANUAL) {
                Spacer(Modifier.height(18.dp))
                Label("언제요?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandChip("오늘", !form.yesterday) { viewModel.editForm { it.copy(yesterday = false) } }
                    BrandChip("어제", form.yesterday) { viewModel.editForm { it.copy(yesterday = true) } }
                }
                Spacer(Modifier.height(18.dp))
                FormField(form.brokerName, { value -> viewModel.editForm { it.copy(brokerName = value.take(50)) } }, "주선사 (없으면 비워 두세요)")
            }
            // 운행을 마칠 때 차가 자동으로 재지만, 틀리거나 비었으면 여기서 고친다
            if (form.kind != FormKind.EXPENSE) {
                Spacer(Modifier.height(18.dp))
                FormField(form.distance, { value -> viewModel.editForm { it.copy(distance = value.filter(Char::isDigit).take(4)) } }, "주행거리 km (모르면 비워 두세요)", KeyboardType.Number)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorNotice(it)
            }
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            PrimaryButton("저장", viewModel::saveForm, enabled = form.canSave, busy = state.isBusy)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(bottom = 8.dp))
}
