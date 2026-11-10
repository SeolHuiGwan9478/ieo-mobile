package com.ieo.mobile.ui.vehicle

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.ui.component.BodyTypeGrid
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.SpecNote
import com.ieo.mobile.ui.component.TonnageChips
import com.ieo.mobile.ui.component.TonnageStepper
import com.ieo.mobile.ui.component.VehicleSpecInputs
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/**
 * 내 차량 (M06, VEH-001): 내 차 한 대를 들어가자마자 고치는 페이지. 차가 없으면 같은 모양으로 등록한다.
 * 번호·톤수·차종·제원을 한 화면에서 고친다. 톤수·차종·제원은 가입 때와 같은 부품을 쓴다
 */
@Composable
fun VehiclesScreen(viewModel: VehiclesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }
    BackHandler(onBack = onBack)
    val form = state.form
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("내 차량", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        if (form == null) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (state.isLoading) SkeletonCards(2) else state.error?.let { ErrorNotice(it) }
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            FormField(form.plateNumber, viewModel::onPlateChange, "차량 번호", placeholder = "12가3456")

            Title("톤수")
            TonnageStepper(form.tonnage, viewModel::stepTonnage)
            Spacer(Modifier.height(12.dp))
            TonnageChips(form.tonnage, viewModel::onTonnage)

            Title("차종")
            BodyTypeGrid(form.bodyType, viewModel::onBodyType)

            Title("제원 (선택)")
            VehicleSpecInputs(form.heightMeters, form.dimensions, viewModel::setHeight, viewModel::onDimensions)
            SpecNote()

            Title("평소 연비 (선택)")
            FuelEconomyInput(form, viewModel::onFuelEconomy)

            state.error?.let { Box(Modifier.padding(top = 14.dp)) { ErrorNotice(it) } }
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            // 번호·톤수·차종을 바꾸면 자동차등록증을 다시 받는다는 걸 저장하기 전에 알려 준다
            if (form.identityChanged) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Orange600, modifier = Modifier.size(20.dp))
                    Text(
                        "번호·톤수·차종을 바꾸면 자동차등록증을 다시 확인해요. 확인 전까지 배차 요청이 막혀요",
                        fontSize = 14.sp, lineHeight = 20.sp, color = Orange700, modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            state.message?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid, modifier = Modifier.padding(bottom = 10.dp)) }
            PrimaryButton(if (form.editing == null) "등록" else "저장", viewModel::save, enabled = form.canSave, busy = state.isBusy)
        }
    }
}

/**
 * 기름값 계산에 쓰는 연비 (VEH-003, REC-011). 차량(Pleos)이 잰 값이 먼저이고, 없으면 이 칸, 이것도 비면 톤수 기본값을 쓴다
 */
@Composable
private fun FuelEconomyInput(form: VehicleForm, onChange: (String) -> Unit) {
    FormField(
        form.fuelEconomy, onChange, "리터당 km", KeyboardType.Decimal,
        placeholder = VehicleForm.formatKmPerLiter(form.tonnage.defaultKmPerLiter),
        trailing = { Text("km/L", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(end = 14.dp)) },
    )
    val measured = form.editing?.measuredKmPerLiter
    val note = when {
        !form.fuelEconomyValid -> "1.5에서 15 사이로 넣어 주세요"
        measured != null -> "차량에서 잰 연비 ${VehicleForm.formatKmPerLiter(measured)}km/L로 기름값을 계산하고 있어요. 차량 연비를 못 받을 때 이 값을 써요."
        else -> "기름값 계산에 써요. 비워 두면 ${form.tonnage.label} 평균 ${VehicleForm.formatKmPerLiter(form.tonnage.defaultKmPerLiter)}km/L로 계산해요."
    }
    Text(
        note, fontSize = 14.sp, lineHeight = 20.sp, color = if (form.fuelEconomyValid) TextSub else Orange700,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
}

/** 홈에서 쓰는 내 차량 한 줄 */
@Composable
fun VehicleSummaryRow(state: VehiclesState, onClick: () -> Unit) {
    val main = state.defaultVehicle
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("내 차량", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f))
        Text(main?.label ?: "등록해 주세요", fontSize = 16.sp, color = TextSub)
        Text("  ›", fontSize = 20.sp, color = TextSub)
    }
}
