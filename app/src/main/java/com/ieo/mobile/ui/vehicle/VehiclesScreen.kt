package com.ieo.mobile.ui.vehicle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.DimensionRange
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.Heading
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

private const val HEIGHT_STEP = 0.1

@Composable
fun VehiclesScreen(viewModel: VehiclesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.closeForm()) onBack() }
    val form = state.form
    if (form != null) {
        VehicleFormScreen(form, state, viewModel)
        return
    }
    Column(Modifier.fillMaxSize().background(CanvasGray).safeDrawingPadding()) {
        BackBar("내 차량", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) SkeletonCards(2)
            if (state.vehicles.isEmpty() && !state.isLoading) {
                EmptyState("등록한 차가 없어요", "차를 등록하면 내 차에 맞는 오더만 보여 드려요")
            }
            state.vehicles.forEach { VehicleCard(it, state.isBusy, viewModel) }
            state.error?.let { ErrorNotice(it) }
        }
        Box(Modifier.padding(20.dp)) { PrimaryButton("차량 추가", viewModel::openAdd) }
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, busy: Boolean, viewModel: VehiclesViewModel) {
    var deleting by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(White, RoundedCornerShape(20.dp)).padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(vehicle.plateNumber, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
            if (vehicle.isDefault) Tag("기본 차량")
            if (vehicle.isLinkedToCar) Tag("차량 앱 연결됨")
        }
        Spacer(Modifier.height(4.dp))
        Text(vehicle.summary, fontSize = 16.sp, color = TextSub)
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuietButton("고치기", { viewModel.openEdit(vehicle) })
            if (!vehicle.isDefault) QuietButton("기본으로", { if (!busy) viewModel.setDefault(vehicle) })
            Spacer(Modifier.weight(1f))
            QuietButton("삭제", { deleting = true })
        }
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("${vehicle.plateNumber} 삭제할까요?") },
            text = { Text("운행 기록이 있는 차량은 지울 수 없어요.", color = TextMid) },
            confirmButton = {
                TextButton(onClick = { deleting = false; viewModel.delete(vehicle) }) { Text("삭제", color = IeoStatus.colors.warning) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("취소", color = TextMid) } },
        )
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Orange700,
        modifier = Modifier.background(Orange50, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** M06 차량 입력 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleFormScreen(form: VehicleForm, state: VehiclesState, viewModel: VehiclesViewModel) {
    val editing = form.editing
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        BackBar("", { viewModel.closeForm() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Heading(if (editing == null) "내 차를 알려 주세요" else "${editing.plateNumber} 고치기")
            Text("실수익 계산과 높이 제한 경고에 써요", fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            if (editing == null) {
                FormField(form.plateNumber, viewModel::onPlateChange, "차량 번호 (경기82사1234)")
                Spacer(Modifier.height(20.dp))
            }
            Label("톤수")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tonnage.entries.forEach { BrandChip(it.label, form.tonnage == it) { viewModel.onTonnage(it) } }
            }
            Spacer(Modifier.height(18.dp))
            Label("차종")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyType.entries.forEach { BrandChip(it.label, form.bodyType == it) { viewModel.onBodyType(it) } }
            }
            Spacer(Modifier.height(18.dp))
            Label("차량 높이 · 적재함 포함")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TonalButton("−", { viewModel.stepHeight(-HEIGHT_STEP) }, modifier = Modifier.width(64.dp))
                Text(
                    form.heightMeters?.let { "%.1fm".format(it) } ?: "모름",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (form.heightMeters == null) TextSub else TextStrong,
                    modifier = Modifier.width(84.dp),
                )
                TonalButton("+", { viewModel.stepHeight(HEIGHT_STEP) }, modifier = Modifier.width(64.dp))
                if (form.heightMeters != null) QuietButton("모름", viewModel::clearHeight)
            }
            Spacer(Modifier.height(18.dp))
            Label("폭·길이·총중량·축 수 (선택) · 안 적으면 톤수 기준으로 계산")
            val d = form.dimensions
            DimensionRow("폭", d.widthCm, VehicleDimensions.WIDTH, ::cmAsMeters) { viewModel.onDimensions(d.copy(widthCm = it)) }
            DimensionRow("길이", d.lengthCm, VehicleDimensions.LENGTH, ::cmAsMeters) { viewModel.onDimensions(d.copy(lengthCm = it)) }
            DimensionRow("총중량", d.totalWeightKg, VehicleDimensions.TOTAL_WEIGHT, ::kgAsTons) { viewModel.onDimensions(d.copy(totalWeightKg = it)) }
            DimensionRow("축 수", d.axleCount, VehicleDimensions.AXLES, { "${it}축" }) { viewModel.onDimensions(d.copy(axleCount = it)) }
            state.error?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IeoStatus.colors.warning, modifier = Modifier.padding(top = 14.dp)) }
        }
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton("취소", { viewModel.closeForm() }, modifier = Modifier.weight(1f))
            PrimaryButton("저장", viewModel::save, enabled = form.canSave, busy = state.isBusy, modifier = Modifier.weight(2f))
        }
    }
}

private fun cmAsMeters(cm: Int): String = "%.1fm".format(cm / 100.0).replace(".0m", "m")

private fun kgAsTons(kg: Int): String = "${kg / 1_000}톤"

@Composable
private fun DimensionRow(label: String, value: Int?, range: DimensionRange, format: (Int) -> String, onChange: (Int?) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong, modifier = Modifier.width(52.dp))
        TonalButton("−", { onChange(range.next(value, -range.step)) }, modifier = Modifier.width(56.dp))
        Text(
            value?.let(format) ?: "모름",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (value == null) TextSub else TextStrong,
            modifier = Modifier.width(66.dp),
        )
        TonalButton("+", { onChange(range.next(value, range.step)) }, modifier = Modifier.width(56.dp))
        if (value != null) QuietButton("모름", { onChange(null) })
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}

/** 홈과 내 정보에서 쓰는 내 차량 한 줄 */
@Composable
fun VehicleSummaryRow(state: VehiclesState, onClick: () -> Unit) {
    val main = state.defaultVehicle
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("내 차량", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f))
        Text(main?.label ?: "등록해 주세요", fontSize = 16.sp, color = TextSub)
        Text("  ›", fontSize = 20.sp, color = TextSub)
    }
}
