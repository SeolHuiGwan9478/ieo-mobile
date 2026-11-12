package com.ieo.mobile.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.White

/** 설명서의 한 장: 번호, 제목, 문장들 */
internal data class GuideSection(val title: String, val lines: List<String>)

/**
 * 요금·결제 안내 (12장 "결제 정책"). 플랜 화면 맨 아래 "?" 줄에서 연다.
 * 요구사항 문서의 결제 정책 표와 같은 내용을 설명서처럼 장으로 나눠 보여 준다
 */
internal val PLAN_GUIDE = listOf(
    GuideSection("플랜과 가격", listOf(
        "베이직은 늘 무료, 프로는 30일에 14,400원이에요.",
        "운행이나 배차 건마다 받는 돈은 없어요. 30일 단위로만 결제해요.",
        "베이직은 지난 장부·내보내기가 잠기고, 대기 증빙은 30일에 3건, 세금계산서는 한 달 10장까지예요. 프로는 대기 증빙 무제한, 세금계산서 한 달 40장이에요.",
    )),
    GuideSection("결제와 자동 갱신", listOf(
        "결제한 날부터 30일 뒤 같은 금액으로 다시 결제돼요.",
        "다시 결제되기 3일 전에 금액과 날짜를 알려 드려요.",
        "가격이 바뀌면 30일 전에 알리고, 다음 결제부터 바뀐 가격이 적용돼요.",
    )),
    GuideSection("무료 체험", listOf(
        "프로 첫 30일은 한 번 무료로 써 볼 수 있어요.",
        "체험이 끝나면 결제 없이 베이직으로 돌아가요. 카드를 받지 않아서 저절로 결제되지 않아요.",
    )),
    GuideSection("해지와 환불", listOf(
        "해지는 언제든 플랜 화면에서 해요. 남은 날까지 쓰고 다음 결제부터 멈춰요. 기간이 끝나기 전에는 해지를 취소할 수 있어요.",
        "결제하고 7일 안에 프로 기능을 한 번도 쓰지 않았으면 전액 돌려드려요. 프로 기능은 대기 증빙 3건 넘게 보내기, 지난 장부 보기, 장부 내보내기, 세금계산서 10장 넘게 발행이에요.",
        "7일이 지났거나 프로 기능을 썼으면 돌려드리지 않고, 남은 날까지 쓰고 다음 결제만 멈춰요.",
    )),
    GuideSection("결제가 안 될 때", listOf(
        "카드 결제가 안 되면 3일 동안 하루 한 번 다시 시도해요.",
        "그래도 안 되면 베이직으로 바뀌어요. 장부와 기록은 지우지 않고 보기만 막혀요. 다시 프로로 바꾸면 그대로 보여요.",
    )),
    GuideSection("보상 쿠폰", listOf(
        "주선사가 상차 2시간 안에 배차를 취소하면 프로 3일 쿠폰을 드려요. 운행 하나에 한 장, 30일에 3장까지예요.",
        "쿠폰은 받은 날부터 90일 안에 [사용하기]를 눌러 써요. 쓰면 지금 플랜 끝에 3일이 더해지고, 베이직이면 바로 프로가 돼요. 쿠폰으로 받은 날이 끝나면 결제 없이 베이직으로 돌아가요.",
        "기사님이 늦게 취소하면 돈이나 쿠폰을 빼지 않아요. 다만 30일에 3번이면 7일 동안 배차 요청이 막혀요.",
    )),
    GuideSection("영수증", listOf(
        "사업자 회원은 구독료 세금계산서를, 개인 회원은 카드 영수증을 받아요.",
    )),
)

@Composable
fun PlanGuideScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("요금·결제 안내", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("지금은 시연용 결제라 실제 돈은 나가지 않아요.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(Orange50, RoundedCornerShape(10.dp)).padding(12.dp))
            PLAN_GUIDE.forEachIndexed { index, section ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp)) {
                    Box(Modifier.size(26.dp).background(Orange600, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Text(section.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 10.dp))
                }
                Column(Modifier.padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.lines.forEach { line -> Text(line, fontSize = 15.sp, lineHeight = 23.sp, color = TextMid) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
