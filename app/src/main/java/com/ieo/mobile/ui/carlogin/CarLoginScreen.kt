package com.ieo.mobile.ui.carlogin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.ScreenTitle
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub

@Composable
fun CarLoginScreen(code: String, viewModel: CarLoginViewModel, onClose: () -> Unit) {
    LaunchedEffect(code) { viewModel.load(code) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        when (val current = state) {
            CarLoginState.Loading -> CircularProgressIndicator()
            is CarLoginState.Asking -> Asking(current, viewModel)
            CarLoginState.Approved -> Finished("차량에 로그인했어요", "차량 화면을 확인해 주세요", onClose)
            CarLoginState.Rejected -> Finished("로그인을 거절했어요", "내 차가 아니라면 잘하셨어요", onClose)
            is CarLoginState.Failed -> Finished("승인할 수 없어요", current.message, onClose)
        }
    }
}

@Composable
private fun Asking(state: CarLoginState.Asking, viewModel: CarLoginViewModel) {
    ScreenTitle("이 차량에\n로그인할까요?")
    Spacer(Modifier.height(8.dp))
    Text("내 차 화면에 같은 코드가 보일 때만 승인하세요", fontSize = 16.sp, color = TextSub)
    Spacer(Modifier.height(28.dp))
    Text(state.request.code, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Orange700)
    Spacer(Modifier.height(16.dp))
    Text(state.request.deviceName, fontSize = 18.sp, color = TextStrong)
    state.request.approxLocation?.let { Text(it, fontSize = 16.sp, color = TextSub) }
    state.error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, fontSize = 16.sp, color = IeoStatus.colors.warning)
    }
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TonalButton("거절", viewModel::reject, enabled = !state.isBusy, modifier = Modifier.weight(1f))
        PrimaryButton("승인", viewModel::approve, enabled = !state.isBusy, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun Finished(title: String, detail: String, onClose: () -> Unit) {
    ScreenTitle(title)
    Spacer(Modifier.height(8.dp))
    Text(detail, fontSize = 16.sp, color = TextSub)
    Spacer(Modifier.height(28.dp))
    PrimaryButton("확인", onClose)
}
