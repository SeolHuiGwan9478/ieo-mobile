package com.ieo.mobile.ui.recommendation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.recommendation.RecommendationPreference
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** null은 조건 없음 */
private val MIN_PROFITS: List<Long?> = listOf(null, 100_000, 200_000, 300_000, 400_000, 500_000)

/** 배차 추천 설정 (03). 고친 게 있을 때만 저장 버튼이 켜진다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationScreen(viewModel: RecommendationViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var leaving by remember { mutableStateOf(false) }
    val tryLeave = { if (state.hasChanges) leaving = true else onBack() }
    BackHandler(onBack = tryLeave)
    val draft = state.draft

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = tryLeave) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("배차 추천", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            if (state.loadFailed) state.error?.let { ErrorNotice(it, viewModel::load) }
            if (state.isLoading) SkeletonCards(2)
            if (state.saved != null) {
                Text("차를 세우면 세운 곳 근처 오더 중 실수익 높은 3건을 차량 화면에 보여 드려요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("추천 받기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
                    Switch(checked = draft.enabled, onCheckedChange = viewModel::setEnabled)
                }
                if (draft.enabled) {
                    Spacer(Modifier.height(24.dp))
                    Label("찾는 범위 · 세운 곳에서")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TonalButton("−5", { viewModel.stepRadius(-RecommendationPreference.RADIUS_STEP_KM) }, modifier = Modifier.width(88.dp))
                        Text("${draft.radiusKm}km", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                        TonalButton("+5", { viewModel.stepRadius(RecommendationPreference.RADIUS_STEP_KM) }, modifier = Modifier.width(88.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Label("최소 실수익")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MIN_PROFITS.forEach { won ->
                            BrandChip(won?.let { "${it / 10_000}만 원" } ?: "조건 없음", draft.minNetProfit == won) { viewModel.setMinNetProfit(won) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Label("제외할 화물")
                    Text("오더 찾기 조건에도 똑같이 적용돼요", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CargoCategory.entries.filter { it.excludable }.forEach { cargo ->
                            BrandChip(cargo.label, cargo in draft.excludedCargo) { viewModel.toggleCargo(cargo) }
                        }
                    }
                }
                if (!state.loadFailed) state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorNotice(it)
                }
                if (state.justSaved) Text("저장했어요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done, modifier = Modifier.padding(top = 14.dp))
            }
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            PrimaryButton("저장", viewModel::save, enabled = state.hasChanges, busy = state.isBusy)
        }
    }

    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text("저장하지 않고 나갈까요?") },
            text = { Text("고친 내용이 사라져요.", fontSize = 16.sp, color = TextMid) },
            confirmButton = {
                TextButton(onClick = { leaving = false; viewModel.discard(); onBack() }) { Text("나가기", fontSize = 16.sp, color = IeoStatus.colors.warning) }
            },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text("계속 수정", fontSize = 16.sp, color = TextMid) } },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(bottom = 10.dp))
}
