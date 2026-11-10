package com.ieo.mobile.ui.credit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.R
import com.ieo.mobile.ui.component.LineGray
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 충전 금액 단추 */
internal val CHARGE_AMOUNTS = listOf(10_000L, 30_000L, 50_000L, 100_000L)

/** 충전·사용 내역 한 줄 (서버가 생기면 그대로 채운다) */
data class CreditHistory(val title: String, val date: String, val amount: Long)

/**
 * 크레딧 (화물맨 예치금처럼 미리 충전해 두고 이용료를 낸다).
 * 지금은 화면만 있다: 결제 연결과 서버 API가 없어서 충전 버튼은 "준비 중"을 알린다.
 * 이용료를 언제 얼마나 빼는지는 아직 정하지 않았다
 */
@Composable
fun CreditScreen(balance: Long, history: List<CreditHistory>, onBack: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf(CHARGE_AMOUNTS[1]) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("크레딧", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // 잔액
            Column(Modifier.fillMaxWidth().background(Orange50, RoundedCornerShape(16.dp)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_credit_coin), contentDescription = null, modifier = Modifier.size(28.dp))
                    Text("보유 크레딧", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMid, modifier = Modifier.padding(start = 8.dp))
                }
                Text(won(balance), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 8.dp))
                Text("크레딧으로 이에오 이용료를 내요", fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(top = 4.dp))
            }

            // 충전 금액
            Text("충전할 금액", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            CHARGE_AMOUNTS.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    pair.forEach { value ->
                        val on = value == amount
                        Box(
                            Modifier.weight(1f).heightIn(min = 56.dp)
                                .background(if (on) Orange50 else White, RoundedCornerShape(12.dp))
                                .border(if (on) 2.dp else 1.dp, if (on) Orange600 else LineGray, RoundedCornerShape(12.dp))
                                .clickable { amount = value; notice = null },
                            contentAlignment = Alignment.Center,
                        ) { Text(won(value), fontSize = 17.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) Orange700 else TextStrong) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("충전 후 잔액", fontSize = 15.sp, color = TextSub, modifier = Modifier.weight(1f))
                Text(won(balance + amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }

            // 결제 수단 (준비 중)
            Text("결제 수단", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            Row(
                Modifier.fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = TextMid, modifier = Modifier.size(24.dp))
                Text("카드 · 계좌이체", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Text("준비 중", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSub)
            }

            // 사용 내역
            Text("충전·사용 내역", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 28.dp, bottom = 4.dp))
            if (history.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_credit_coin), contentDescription = null, modifier = Modifier.size(40.dp))
                    Text("아직 내역이 없어요", fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(top = 10.dp))
                }
            } else {
                history.forEach { HistoryRow(it) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            notice?.let {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(CanvasGray, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = TextMid, modifier = Modifier.size(20.dp))
                    Text(it, fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(start = 8.dp))
                }
            }
            PrimaryButton("${won(amount)} 충전하기", { notice = "결제 연결은 준비 중이에요. 곧 열어 드릴게요" })
        }
    }
}

@Composable
private fun HistoryRow(item: CreditHistory) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 16.sp, color = TextStrong)
            Text(item.date, fontSize = 14.sp, color = TextSub)
        }
        Text((if (item.amount > 0) "+" else "") + won(item.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (item.amount > 0) Orange700 else TextStrong)
    }
}

private fun won(value: Long) = "%,d원".format(value)
