package com.ieo.mobile.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.ledger.InvoiceParty
import com.ieo.mobile.domain.ledger.TaxInvoice
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.LineGray
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.ruled
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/**
 * 운행 한 건의 운송료 세금계산서 (LEDG-041~045). 발행 전에는 초안을 보여 주고 빈 정보가 없으면 [발행하기].
 * 지금은 시연용 발행이라 국세청에 보내지 않는다고 위에 알린다
 */
@Composable
fun TaxInvoiceScreen(invoice: TaxInvoice, busy: Boolean, error: String?, onBack: () -> Unit, onIssue: () -> Unit) {
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("세금계산서", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (invoice.demo) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Orange700, modifier = Modifier.size(20.dp))
                    Text("시연용 발행이에요. 국세청에는 보내지 않아요.", fontSize = 14.sp, color = Orange700, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(if (invoice.issued) "발행했어요" else "발행 전 확인", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (invoice.issued) Orange700 else TextSub)
            Text(invoice.item, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

            Column(Modifier.fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountLine("작성일", "${invoice.writeDate.year}.${invoice.writeDate.monthValue}.${invoice.writeDate.dayOfMonth}")
                PartyLines("공급자 (나)", invoice.supplier)
                PartyLines("받는 곳 (주선사)", invoice.buyer)
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).ruled().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountLine("운임", formatWon(invoice.fareAmount))
                if (invoice.waitFeeAmount > 0) AmountLine("대기비", formatWon(invoice.waitFeeAmount))
                AmountLine("공급가액", formatWon(invoice.supplyAmount))
                AmountLine("부가세 (10%)", formatWon(invoice.vat))
                AmountLine("합계", formatWon(invoice.totalAmount), strong = true)
            }
            invoice.approvalNumber?.let {
                Text("승인번호 $it", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(top = 4.dp))
            }
            if (invoice.missing.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 14.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text("발행하려면 채워야 해요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    invoice.missing.forEach { Text("· $it", fontSize = 15.sp, color = TextStrong, modifier = Modifier.padding(top = 4.dp)) }
                    Text("사업자 정보는 내 정보 > 사업자 정보에서 넣을 수 있어요.", fontSize = 14.sp, color = TextMid, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Text("운임은 부가세 별도로 보고 10%를 더했어요.", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp))
            error?.let { ErrorNotice(it) }
        }
        if (!invoice.issued) {
            Column(Modifier.background(White).padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(if (invoice.demo) "발행하기 (시연용)" else "발행하기", onIssue, enabled = invoice.missing.isEmpty(), busy = busy)
            }
        }
    }
}

@Composable
private fun PartyLines(title: String, party: InvoiceParty) {
    Column {
        Text(title, fontSize = 14.sp, color = TextSub)
        Text(party.name ?: "-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 2.dp))
        val detail = listOfNotNull(party.businessNumber?.let(::bizNumber), party.representative).joinToString(" · ")
        if (detail.isNotEmpty()) Text(detail, fontSize = 14.sp, color = TextMid)
    }
}

/** 1234567891 → 123-45-67891 */
private fun bizNumber(digits: String) = if (digits.length == 10) "${digits.take(3)}-${digits.substring(3, 5)}-${digits.drop(5)}" else digits
