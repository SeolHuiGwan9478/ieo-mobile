package com.ieo.mobile.ui.login

import com.ieo.mobile.domain.vehicle.PlateNumber
import androidx.compose.runtime.saveable.rememberSaveable
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.ui.component.SpecNote
import com.ieo.mobile.ui.component.VehicleSpecInputs
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.component.TruckIcon
import com.ieo.mobile.ui.component.LineGray
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import android.widget.Toast
import kotlinx.coroutines.async
import com.ieo.mobile.ui.theme.Orange500
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import com.ieo.mobile.ui.component.ruled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.R
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.auth.PhoneFormat
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.Heading
import com.ieo.mobile.ui.component.IeoWordmark
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.StepLabel
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val ErrorText = Color(0xFFB91C1C)
private val ErrorBorder = Color(0xFFEF4444)
private val ErrorFill = Color(0xFFFEF2F2)
private val LineGray = Color(0xFFE5E8EB)
private val OffGray = Color(0xFFD1D6DB)

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<Consent?>(null) }

    BackHandler(enabled = detail != null || state.step !in setOf(LoginStep.START, LoginStep.DONE)) {
        if (detail != null) detail = null else viewModel.back()
    }

    val open = detail
    when {
        open != null -> TermsDetailScreen(open, onBack = { detail = null }, onAgree = {
            if (open !in state.consents) viewModel.toggleConsent(open)
            detail = null
        })
        state.step == LoginStep.START -> StartScreen(viewModel::start)
        state.step == LoginStep.DONE -> DoneScreen(state, viewModel::finish)
        else -> StepScaffold(onBack = { viewModel.back() }) {
            when (state.step) {
                LoginStep.PHONE -> PhoneStep(state, viewModel)
                LoginStep.CODE -> CodeStep(state, viewModel)
                LoginStep.TERMS -> TermsStep(state, viewModel) { detail = it }
                LoginStep.NAME -> NameStep(state, viewModel)
                LoginStep.BODY -> BodyStep(state, viewModel)
                else -> VehicleStep(state, viewModel)
            }
        }
    }
    state.existing?.let { ExistingAccountDialog(it, state, viewModel) }
}

/** 나타나는 동안 눌리지 않게 막아 두지만, 색은 활성 상태와 같게 보이도록 한다 */
@Composable
private fun startButtonColors(container: Color, content: Color) = ButtonDefaults.buttonColors(
    containerColor = container,
    contentColor = content,
    disabledContainerColor = container,
    disabledContentColor = content,
)

/** M01 시작. 차량 앱의 시작 화면(SplashScreen)과 같다: 로고, 문구, 시작하기 버튼이 차례로 나타난다 */
@Composable
internal fun StartScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val logoAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(24f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
        delay(150)
        val fade = async { taglineAlpha.animateTo(1f, tween(durationMillis = 800)) }
        taglineOffset.animateTo(0f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
        fade.await()
        delay(300)
        buttonAlpha.animateTo(1f, tween(durationMillis = 500))
    }

    Box(Modifier.fillMaxSize().background(Orange500).safeDrawingPadding()) {
        // 아래 버튼 묶음(약 170dp)을 뺀 공간의 가운데에 둔다
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(bottom = 170.dp),
        ) {
            // 오른쪽 마침표 때문에 "ieo" 글자가 왼쪽으로 쏠려 보여서 조금 오른쪽으로 옮긴다
            IeoWordmark(width = 200.dp, color = White, modifier = Modifier.offset(x = 7.dp).graphicsLayer { alpha = logoAlpha.value })
            Spacer(Modifier.height(28.dp))
            Text(
                "끝나는 곳에서, 다음이 이어집니다",
                color = White.copy(alpha = 0.9f),
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = taglineAlpha.value
                    translationY = taglineOffset.value.dp.toPx()
                },
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                .fillMaxWidth()
                .graphicsLayer { alpha = buttonAlpha.value },
        ) {
            // TODO: 앱용 Google 키를 받으면 로그인을 연결한다
            Button(
                onClick = { Toast.makeText(context, "구글 로그인은 준비 중이에요. 시작하기를 눌러 주세요", Toast.LENGTH_SHORT).show() },
                enabled = buttonAlpha.value > 0.5f,
                shape = RoundedCornerShape(12.dp),
                colors = startButtonColors(White, TextStrong),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Google로 계속하기", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onStart,
                enabled = buttonAlpha.value > 0.5f,
                shape = RoundedCornerShape(12.dp),
                // 주황 바탕을 비치게 채우고 얇은 흰 테두리로 누르는 버튼임을 보여 준다
                colors = startButtonColors(White.copy(alpha = 0.22f), White),
                border = BorderStroke(1.dp, White.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("시작하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepScaffold(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            content = content,
        )
    }
}

/** V01 번호 입력 */
@Composable
private fun ColumnScope.PhoneStep(state: LoginState, viewModel: LoginViewModel) {
    StepLabel("1 / 5 · 휴대폰 인증")
    Heading("휴대폰 번호를\n입력해 주세요")
    Spacer(Modifier.height(26.dp))
    Text("휴대폰 번호", fontSize = 16.sp, color = Orange700)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = state.phone,
        onValueChange = viewModel::onPhoneChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextStrong),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = PhoneHyphenTransformation,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).focusRequester(focus),
    )
    Box(Modifier.fillMaxWidth().height(2.dp).background(Orange600))
    ErrorLine(state.error)
    Spacer(Modifier.weight(1f))
    PrimaryButton("인증번호 받기", viewModel::requestCode, enabled = state.canRequestCode, busy = state.isBusy)
    Spacer(Modifier.height(12.dp))
}

/** V02·V03 인증번호 입력 */
@Composable
private fun ColumnScope.CodeStep(state: LoginState, viewModel: LoginViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.codeExpiresAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= state.codeExpiresAtMillis) viewModel.onCodeTimeUp()
            delay(1_000)
        }
    }
    val left = ((state.codeExpiresAtMillis - now) / 1_000).coerceAtLeast(0)
    val resendLeft = ((state.resendAtMillis - now) / 1_000).coerceAtLeast(0)

    StepLabel("1 / 5 · 휴대폰 인증")
    Heading("인증번호 6자리를\n입력해 주세요")
    Spacer(Modifier.height(8.dp))
    Row {
        Text(PhoneFormat.hyphen(state.phone), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text("로 보냈어요", fontSize = 16.sp, color = TextSub)
    }
    state.demoCode?.let { Text("시연용 인증번호 $it", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 6.dp)) }
    Spacer(Modifier.height(30.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("인증번호", fontSize = 15.sp, color = if (state.codeRejected) ErrorText else TextSub)
        Text("%d:%02d".format(left / 60, left % 60), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700)
    }
    Spacer(Modifier.height(10.dp))
    CodeBoxes(state.code, state.codeRejected, viewModel::onCodeChange)
    ErrorLine(state.error)
    Spacer(Modifier.height(12.dp))
    when {
        state.codeExpired -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(CanvasGray, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("인증 시간이 지났어요", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f))
            TonalButton("재요청", viewModel::requestCode, enabled = !state.isBusy)
        }
        resendLeft > 0 -> Text("인증번호 재요청 · %d:%02d 후".format(resendLeft / 60, resendLeft % 60), fontSize = 15.sp, color = TextSub)
        else -> Text("인증번호 재요청", fontSize = 15.sp, color = TextMid, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { viewModel.requestCode() })
    }
    Spacer(Modifier.weight(1f))
    PrimaryButton("확인", viewModel::verify, enabled = state.canVerify, busy = state.isBusy)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun CodeBoxes(code: String, rejected: Boolean, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = code,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        // 문자로 온 인증번호를 키보드가 제안하게 한다
        modifier = Modifier.focusRequester(focus).semantics { contentType = ContentType.SmsOtpCode },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(LoginState.CODE_LENGTH) { index ->
                    val shape = RoundedCornerShape(12.dp)
                    val boxModifier = when {
                        rejected -> Modifier.background(ErrorFill, shape).border(2.dp, ErrorBorder, shape)
                        index == code.length -> Modifier.background(White, shape).border(2.dp, Orange600, shape)
                        else -> Modifier.background(CanvasGray, shape)
                    }
                    Box(boxModifier.weight(1f).height(60.dp), contentAlignment = Alignment.Center) {
                        Text(
                            code.getOrNull(index)?.toString().orEmpty(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rejected) ErrorText else TextStrong,
                        )
                    }
                }
            }
        },
    )
}

/** V04 약관 동의 */
@Composable
private fun ColumnScope.TermsStep(state: LoginState, viewModel: LoginViewModel, onDetail: (Consent) -> Unit) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepLabel("2 / 5 · 약관 동의")
        Heading("약관에\n동의해 주세요")
        Spacer(Modifier.height(22.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
                .border(1.5.dp, LineGray, RoundedCornerShape(16.dp))
                .clickable(onClick = viewModel::toggleAllConsents)
                .padding(16.dp),
        ) {
            CheckCircle(state.allAgreed, 26)
            Column {
                Text("모두 동의하기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Text("선택 항목 포함", fontSize = 14.sp, color = TextSub)
            }
        }
        Spacer(Modifier.height(6.dp))
        Consent.entries.forEach { consent ->
            ConsentRow(consent, consent in state.consents, onToggle = { viewModel.toggleConsent(consent) }, onDetail = { onDetail(consent) })
        }
    }
    PrimaryButton("다음", viewModel::agreeToTerms, enabled = state.canAgree)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ConsentRow(consent: Consent, checked: Boolean, onToggle: () -> Unit, onDetail: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp).heightIn(min = 48.dp),
    ) {
        CheckCircle(checked, 26)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (consent.required) "필수" else "선택",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (consent.required) Orange700 else TextSub,
                )
                Text(consent.label, fontSize = 16.sp, color = TextStrong)
            }
            consent.hint?.let { Text(it, fontSize = 14.sp, color = TextSub) }
        }
        Text("보기 ›", fontSize = 15.sp, color = TextMid, modifier = Modifier.clickable(onClick = onDetail).padding(vertical = 14.dp, horizontal = 6.dp))
    }
}

@Composable
private fun CheckCircle(checked: Boolean, sizeDp: Int) {
    val modifier = Modifier.size(sizeDp.dp)
    if (checked) {
        Box(modifier.background(Orange600, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, contentDescription = "동의함", tint = White, modifier = Modifier.size((sizeDp * 0.6f).dp))
        }
    } else {
        Box(modifier.border(2.dp, OffGray, CircleShape))
    }
}

/** V05 약관 상세 */
@Composable
private fun TermsDetailScreen(consent: Consent, onBack: () -> Unit, onAgree: () -> Unit) {
    val detail = termsDetail(consent)
    StepScaffold(onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Heading(consent.label)
            Spacer(Modifier.height(4.dp))
            Text(if (consent.required) "필수" else "선택", fontSize = 15.sp, color = TextSub)
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().background(CanvasGray, RoundedCornerShape(14.dp)).padding(16.dp)) {
                Text("한 줄 요약", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextSub)
                Spacer(Modifier.height(4.dp))
                Text(detail.summary, fontSize = 16.sp, lineHeight = 22.sp, color = TextStrong)
            }
            detail.sections.forEach {
                Spacer(Modifier.height(20.dp))
                Text(it.heading, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(it.body, fontSize = 16.sp, lineHeight = 22.sp, color = TextMid)
            }
        }
        PrimaryButton("동의하기", onAgree)
        Spacer(Modifier.height(12.dp))
    }
}

/** V06을 나눴다: 한 화면에 하나만 묻는다 */
@Composable
private fun ColumnScope.NameStep(state: LoginState, viewModel: LoginViewModel) {
    StepLabel("3 / 5 · 이름")
    Heading("이름을 알려 주세요")
    Spacer(Modifier.height(26.dp))
    FormField(state.name, viewModel::onNameChange, "이름")
    ErrorLine(state.error)
    Spacer(Modifier.weight(1f))
    PrimaryButton("다음", viewModel::nameDone, enabled = state.name.isNotBlank())
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ColumnScope.VehicleStep(state: LoginState, viewModel: LoginViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepLabel("4 / 5 · 내 차")
        Heading("차량 번호와\n톤수를 알려 주세요")
        Spacer(Modifier.height(22.dp))
        FormField(state.plateNumber, viewModel::onPlateChange, "차량 번호", placeholder = "12가3456")
        Spacer(Modifier.height(26.dp))
        ChoiceTitle("톤수")
        TonnageStepper(state.tonnage, viewModel::stepTonnage)
        Spacer(Modifier.height(14.dp))
        // 바로 고르기: 한 줄로 밀어서 본다
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tonnage.entries.forEach { BrandChip(it.label, state.tonnage == it) { viewModel.onTonnage(it) } }
        }
        Spacer(Modifier.height(28.dp))
        ChoiceTitle("제원 (선택)")
        VehicleSpecInputs(state.heightMeters, state.dimensions, viewModel::onHeight, viewModel::onDimensions)
        SpecNote()
        ErrorLine(state.error)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TonalButton("나중에", { viewModel.signUp(withVehicle = false) }, enabled = !state.isBusy, modifier = Modifier.weight(1f))
        PrimaryButton("다음", viewModel::vehicleDone, enabled = PlateNumber.isValid(state.plateNumber), modifier = Modifier.weight(2f))
    }
    Spacer(Modifier.height(12.dp))
}

/** 큰 톤수 표시 양옆의 −/+ */
@Composable
private fun TonnageStepper(tonnage: Tonnage, onStep: (Int) -> Unit) {
    val first = tonnage == Tonnage.entries.first()
    val last = tonnage == Tonnage.entries.last()
    Row(
        Modifier.fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", "톤수 내리기", enabled = !first) { onStep(-1) }
        Text(tonnage.label, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        StepperButton("+", "톤수 올리기", enabled = !last) { onStep(1) }
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(if (enabled) CanvasGray else White, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = if (enabled) TextStrong else LineGray)
    }
}

/** 차량 종류: 트럭 그림이 있는 네모 칸에서 고른다 */
@Composable
private fun ColumnScope.BodyStep(state: LoginState, viewModel: LoginViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepLabel("5 / 5 · 차량 종류")
        Heading("어떤 차를 운행하세요?")
        Text("${state.tonnage.label} · ${state.plateNumber.trim()}", fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        BodyType.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                pair.forEach { type -> BodyTile(type, state.bodyType == type, Modifier.weight(1f)) { viewModel.onBodyType(type) } }
            }
        }
        ErrorLine(state.error)
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("가입 완료", { viewModel.signUp() }, busy = state.isBusy)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun BodyTile(type: BodyType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .aspectRatio(1.15f)
            .background(if (selected) Orange50 else White, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Orange600 else LineGray, shape)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        TruckIcon(type, width = 84.dp)
        Text(type.label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Orange700 else TextStrong)
        Spacer(Modifier.weight(1f))
    }
}

/** V07 가입 완료 */
@Composable
private fun DoneScreen(state: LoginState, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().padding(24.dp)) {
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(64.dp).background(Orange600, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Heading("${state.name.trim()} 님,\n가입을 마쳤어요")
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().background(CanvasGray, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryLine("휴대폰", PhoneFormat.hyphen(state.phone))
            SummaryLine("역할 · 차량", vehicleSummary(state))
        }
        Spacer(Modifier.height(12.dp))
        Text("차량에서는 화면의 QR을 찍어 로그인해요", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.weight(1f))
        PrimaryButton("시작하기", onFinish)
    }
}

private fun vehicleSummary(state: LoginState): String =
    if (state.plateNumber.isBlank()) "차주 · 차량은 나중에" else "차주 · ${state.tonnage.label} ${state.bodyType.label}"

/** V09 이미 가입된 계정 */
@Composable
private fun ExistingAccountDialog(account: ExistingAccount, state: LoginState, viewModel: LoginViewModel) {
    Dialog(onDismissRequest = {}) {
        Column(Modifier.fillMaxWidth().ruled().padding(vertical = 24.dp)) {
            Text("이미 가입된 계정이 있어요", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Spacer(Modifier.height(8.dp))
            Text("이 계정으로 로그인해요.", fontSize = 16.sp, lineHeight = 22.sp, color = TextMid)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().background(CanvasGray, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(account.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                SummaryLine("휴대폰", PhoneFormat.hyphen(state.phone))
                SummaryLine("역할", "차주")
                accountDates(account)?.let { SummaryLine("가입 · 마지막 로그인", it) }
            }
            ErrorLine(state.error)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("로그인", viewModel::login, busy = state.isBusy)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { QuietButton("처음으로", viewModel::restart) }
        }
    }
}

private val ShortDate = DateTimeFormatter.ofPattern("M월 d일").withZone(ZoneId.of("Asia/Seoul"))

private fun accountDates(account: ExistingAccount): String? {
    val joined = account.joinedAt?.let(ShortDate::format) ?: return null
    val last: Instant? = account.lastLoginAt
    return if (last == null) joined else "$joined · ${ShortDate.format(last)}"
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 16.sp, color = TextSub)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
    }
}

@Composable
private fun ChoiceTitle(text: String) {
    Text(text, fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Sub(text: String) {
    Text(text, fontSize = 16.sp, lineHeight = 22.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ErrorLine(error: String?) {
    // 한 줄로 보이게, 넘치면 글자를 줄인다
    error?.let {
        BasicText(
            it,
            style = TextStyle(fontWeight = FontWeight.Medium, color = ErrorText),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 15.sp),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** 입력한 숫자에 하이픈을 그려 준다. 값에는 숫자만 남는다 */
private object PhoneHyphenTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = PhoneFormat.hyphen(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var seen = 0
                formatted.forEachIndexed { index, c ->
                    if (seen == offset) return index
                    if (c != '-') seen++
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it != '-' }
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(formatted), mapping)
    }
}
