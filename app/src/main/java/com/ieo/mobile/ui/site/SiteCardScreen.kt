package com.ieo.mobile.ui.site

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import com.ieo.mobile.ui.component.InfoTile
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.ieo.mobile.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.site.HeightCheck
import com.ieo.mobile.domain.site.HeightLevel
import com.ieo.mobile.domain.site.SiteCard
import com.ieo.mobile.domain.site.SiteNote
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.notification.timeAgo
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

private val CardShape = RoundedCornerShape(20.dp)
private val BlockedBackground = Color(0xFFFDECEC)
private val BlockedText = Color(0xFFB3261E)

private fun meters(value: Double): String = "${value.toString().removeSuffix(".0")}m"

private fun yesNo(value: Boolean?): String = when (value) {
    true -> "있어요"
    false -> "없어요"
    null -> "몰라요"
}

/** 배차·오더의 상하차지 줄에 붙는 입구. 손끝 크기를 지킨다 */
@Composable
fun SiteInfoLink(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        // 왼쪽은 위 주소 글자와 줄을 맞춘다. 누르는 자리는 오른쪽으로 넉넉히
        modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp).clickable(onClick = onClick).padding(end = 14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_site_info), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text("현장 정보", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700)
    }
}

/** 배차·오더 상세에서 여는 상하차지 정보 카드 */
@Composable
fun SiteCardScreen(viewModel: SiteCardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back = { if (state.mode == SiteMode.VIEW) viewModel.close() else viewModel.backToView() }
    BackHandler(onBack = back)
    val title = when (state.mode) {
        SiteMode.VIEW -> "현장 정보"
        SiteMode.EDIT_INFO -> "정보 수정"
        SiteMode.WRITE_NOTE -> "메모 남기기"
    }
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        val card = state.card
        when {
            card == null -> Column(Modifier.padding(horizontal = 20.dp)) {
                state.loadError?.let { ErrorNotice(it, viewModel::retry) }
                if (state.isLoading) SkeletonCards(3)
            }
            state.mode == SiteMode.VIEW -> ViewMode(card, state, viewModel)
            state.mode == SiteMode.EDIT_INFO -> EditMode(state, viewModel)
            else -> NoteMode(state, viewModel)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ViewMode(card: SiteCard, state: SiteCardState, viewModel: SiteCardViewModel) {
    val context = LocalContext.current
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(card.name, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Text(card.address, fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        }
        HeightNotice(card.height)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(R.drawable.ic_site_height, "높이 제한", card.attributes.heightLimitM?.let(::meters) ?: "몰라요", Modifier.weight(1f), card.attributes.heightLimitM != null)
            InfoTile(R.drawable.ic_site_wait, "평균 대기", card.stats?.avgWaitMinutes?.let { "${it}분" } ?: "기록 부족", Modifier.weight(1f), card.stats?.avgWaitMinutes != null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(R.drawable.ic_site_forklift, "지게차", yesNo(card.attributes.forklift), Modifier.weight(1f), card.attributes.forklift != null)
            InfoTile(R.drawable.ic_site_parking, "주차", yesNo(card.attributes.parking), Modifier.weight(1f), card.attributes.parking != null)
        }
        card.attributes.updatedAt?.let { Text("${timeAgo(it)} 기사님 제보", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(horizontal = 4.dp)) }
        state.saved?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done, modifier = Modifier.padding(horizontal = 4.dp)) }
        TonalButton("지도", {
            val query = Uri.encode(card.name)
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${card.latitude},${card.longitude}?q=${card.latitude},${card.longitude}($query)"))) }
        }, modifier = Modifier.fillMaxWidth(), colorIcon = R.drawable.ic_site_map)
        Text("기사님들의 메모", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        if (card.notes.isEmpty()) {
            Section { Text("아직 메모가 없어요. 첫 메모를 남겨 주세요", fontSize = 16.sp, color = TextMid) }
        }
        card.notes.forEach { NoteRow(it) }
        Spacer(Modifier.height(4.dp))
    }
    Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TonalButton("정보 수정", viewModel::startEdit, modifier = Modifier.weight(1f), icon = Icons.Outlined.Edit)
        PrimaryButton("메모", viewModel::startNote, modifier = Modifier.weight(1f), icon = Icons.Outlined.EditNote)
    }
}

/** 못 들어가는 곳은 색만이 아니라 말로 먼저 알린다 */
@Composable
private fun HeightNotice(height: HeightCheck) {
    val vehicle = height.vehicleM ?: return
    val limit = height.limitM ?: return
    val (headline, background, color) = when (height.level) {
        HeightLevel.BLOCKED -> Triple("내 차는 높아서 못 들어가요", BlockedBackground, BlockedText)
        HeightLevel.CAUTION -> Triple("높이 여유가 20cm도 안 돼요", Orange50, Orange700)
        else -> return
    }
    Column(Modifier.fillMaxWidth().background(background, CardShape).padding(18.dp)) {
        Text(headline, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text("내 차 ${meters(vehicle)} · 제한 ${meters(limit)}", fontSize = 16.sp, color = TextStrong)
    }
}

@Composable
private fun NoteRow(note: SiteNote) {
    Section {
        Text(note.text, fontSize = 17.sp, lineHeight = 24.sp, color = TextStrong)
        val parts = listOfNotNull(
            timeAgo(note.createdAt),
            "내가 쓴 메모".takeIf { note.mine },
            "도움 돼요 ${note.helpfulCount}".takeIf { note.helpfulCount > 0 },
        )
        Text(parts.joinToString(" · "), fontSize = 14.sp, color = TextSub)
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

@Composable
private fun YesNoChoice(label: String, value: Boolean?, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, fontSize = 16.sp, color = TextSub)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandChip("있어요", value == true) { onChange(true) }
            BrandChip("없어요", value == false) { onChange(false) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EditMode(state: SiteCardState, viewModel: SiteCardViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).background(White).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("아는 것만 고르면 돼요. 다른 기사님도 함께 봐요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        Column {
            FormField(state.draft.height, viewModel::setHeight, "높이 제한 (m)", KeyboardType.Decimal, Modifier.fillMaxWidth())
            Text("예: 3.5", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(top = 6.dp))
        }
        YesNoChoice("지게차", state.draft.forklift, viewModel::setForklift)
        YesNoChoice("차를 대고 기다릴 곳", state.draft.parking, viewModel::setParking)
        state.error?.let { ErrorNotice(it) }
    }
    Column(Modifier.fillMaxWidth().background(White).padding(horizontal = 24.dp, vertical = 12.dp)) {
        PrimaryButton("저장", viewModel::saveInfo, busy = state.isBusy)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.NoteMode(state: SiteCardState, viewModel: SiteCardViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).background(White).padding(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::setNote,
            placeholder = { Text("예: 정문 말고 후문으로 들어가요", fontSize = 17.sp, color = TextSub) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
        )
        Text("${state.note.length} / ${SiteCardState.MAX_NOTE}자", fontSize = 14.sp, color = TextSub)
        Text(
            "다른 기사님에게도 보여요. 이름은 나오지 않아요.\n남긴 뒤에는 지울 수 없으니 전화번호 같은 개인정보는 적지 말아 주세요.",
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = TextMid,
        )
        state.error?.let { ErrorNotice(it) }
    }
    Column(Modifier.fillMaxWidth().background(White).padding(horizontal = 24.dp, vertical = 12.dp)) {
        PrimaryButton("남기기", viewModel::saveNote, enabled = state.canSaveNote, busy = state.isBusy)
    }
}
