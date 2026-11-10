package com.ieo.mobile.ui.my

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FieldAction
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.PhoneHyphenTransformation
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 내 정보 수정: 이름과 휴대폰 번호. 번호는 새 번호로 인증번호를 확인해야 바뀐다 */
@Composable
fun ProfileEditScreen(viewModel: ProfileEditViewModel, onBack: () -> Unit, onSaved: (Account) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("내 정보 수정", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            FormField(state.name, viewModel::onName, "이름")
            Spacer(Modifier.height(18.dp))
            // 인증 요청·확인 버튼은 칸 안쪽 오른쪽에 둔다
            FormField(
                state.phone, viewModel::onPhone, "휴대폰 번호", KeyboardType.Phone,
                visualTransformation = PhoneHyphenTransformation,
                trailing = when {
                    !state.phoneChanged -> null
                    state.phoneVerified -> { { Icon(Icons.Filled.CheckCircle, contentDescription = "인증됨", tint = Orange700, modifier = Modifier.padding(end = 12.dp).size(22.dp)) } }
                    else -> { { FieldAction(if (state.codeSent) "재요청" else "인증 요청", viewModel::sendCode, enabled = state.canSendCode) } }
                },
            )
            if (state.phoneChanged) {
                Text(
                    if (state.phoneVerified) "인증을 마쳤어요. 저장하면 다음부터 이 번호로 로그인해요."
                    else "새 번호로 인증해야 바꿀 수 있어요.",
                    fontSize = 14.sp, lineHeight = 20.sp, color = if (state.phoneVerified) Orange700 else TextSub, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
            if (state.phoneChanged && state.codeSent && !state.phoneVerified) {
                Spacer(Modifier.height(14.dp))
                FormField(
                    state.code, viewModel::onCode, "인증번호 6자리", KeyboardType.NumberPassword,
                    trailing = { FieldAction("확인", viewModel::verify, enabled = state.canVerify) },
                )
                state.demoCode?.let { Text("시연용 인증번호 $it", fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
            }
            state.error?.let { Box(Modifier.padding(top = 14.dp)) { ErrorNotice(it) } }
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("저장", { viewModel.save(onSaved) }, enabled = state.canSave, busy = state.isBusy)
        }
    }
}
