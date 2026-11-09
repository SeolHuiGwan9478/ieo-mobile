package com.ieo.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// 브랜드색: 주황 (design/README.md 색상 팔레트). Postman 주황에서 출발했다
val Orange50 = Color(0xFFFFF3EE)   // 선택된 메뉴·강조 바탕
val Orange100 = Color(0xFFFFE2D6)
val Orange500 = Color(0xFFFF6C37)  // 로고, 아이콘, 강조 표시
val Orange600 = Color(0xFFF25A22)  // 주요 버튼 바탕 (흰 굵은 글자 24sp 이상)
val Orange700 = Color(0xFFC2410C)  // 흰 바탕 위 주황 글자

// 기본색: 무채색
val Black = Color(0xFF000000)
val Gray950 = Color(0xFF121212)
val Gray900 = Color(0xFF1E1E1E)
val Gray800 = Color(0xFF2C2C2C)
val Gray500 = Color(0xFF6B6B6B)
val Gray400 = Color(0xFFA3A3A3)
val Gray100 = Color(0xFFEDEDED)
val White = Color(0xFFFFFFFF)

// 홈·목록 화면용 (A안 시안): 연회색 바탕에 흰 묶음, 글자는 진한 남회색 계열
val CanvasGray = Color(0xFFF2F4F6)   // 화면 바탕, 보조 버튼, 시각 칸
val HoverGray = Color(0xFFF9FAFB)
val TextStrong = Color(0xFF191F28)   // 제목·금액
val TextMid = Color(0xFF4E5968)      // 보조 버튼 글자
val TextSub = Color(0xFF5F6B78)      // 설명 글자
val WarningPale = Color(0xFFFDECEC)  // 미수금 아이콘 바탕
val DrivingPale = Color(0xFFE8EFFD)  // 운행 상태 꼬리표 바탕
val WaitingPale = Color(0xFFFEF9C3)  // 대기 상태 꼬리표 바탕

// 키워드 태그(차량·결제·지급): 주황과 잘 어울리는 색 중에서 키워드 성격에 맞게 고른다.
// 상태색과 헷갈리지 않게 모두 옅은 바탕 + 진한 글자로 쓴다
data class TagColor(val background: Color, val content: Color)

object IeoTag {
    val Navy = TagColor(Color(0xFFE8ECF4), Color(0xFF1E2A44))   // 차량: 단단한 장비
    val Teal = TagColor(Color(0xFFD5F5F0), Color(0xFF0F766E))   // 선불: 바로 받는 돈
    val Sand = TagColor(Color(0xFFF5EBDD), Color(0xFF7C5A2E))   // 인수증: 종이 서류
    val Sage = TagColor(Color(0xFFE6EFE2), Color(0xFF3F5F31))   // 지급이 빠른 주선사 (15일 이하)
    val Plum = TagColor(Color(0xFFF3E6F5), Color(0xFF74307F))   // 지급이 늦은 주선사 (20일 초과)
    val Gray = TagColor(CanvasGray, TextMid)                    // 보통이거나 판단할 근거가 없을 때
}

// 상태색: 운행 상황을 알릴 때만 사용
data class StatusColors(
    val driving: Color,
    val waiting: Color,
    val done: Color,
    val warning: Color,
)

val DarkStatus = StatusColors(
    driving = Color(0xFF60A5FA),
    waiting = Color(0xFFFACC15),
    done = Color(0xFF4ADE80),
    warning = Color(0xFFF87171),
)

val LightStatus = StatusColors(
    driving = Color(0xFF1D4ED8),
    // 대기는 브랜드 주황과 헷갈리지 않도록 노랑 계열로 둔다
    waiting = Color(0xFFA16207),
    done = Color(0xFF15803D),
    warning = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = Orange500,
    onPrimary = White,
    secondary = Gray800,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    outline = Gray800,
    error = DarkStatus.warning,
)

private val LightColors = lightColorScheme(
    primary = Orange600,
    onPrimary = White,
    secondary = Gray100,
    onSecondary = Black,
    background = White,
    onBackground = Black,
    surface = Gray100,
    onSurface = Black,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    outline = Gray100,
    error = LightStatus.warning,
    // 창과 고르기 칩이 기본 보라색으로 나오지 않게 브랜드 색을 준다
    surfaceContainerHigh = White,
    secondaryContainer = Orange50,
    onSecondaryContainer = Orange700,
)

/**
 * 앱 테마. 기본은 라이트 모드(흰 바탕, 진한 글씨, 주황 버튼)다.
 * 다크 색상은 남겨 두었으니, 나중에 밤에 어둡게 바꾸려면 darkTheme에 isSystemInDarkTheme()을 넘기면 된다.
 */
@Composable
fun IeoTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = IeoTypography,
            content = content,
        )
    }
}

/** 금액 자릿수가 줄마다 맞도록 숫자 폭을 고정한다 */
private val IeoTypography = Typography().let { it.copy(bodyLarge = it.bodyLarge.copy(fontFeatureSettings = "tnum", letterSpacing = 0.sp)) }

private val LocalStatusColors = staticCompositionLocalOf { LightStatus }

/** 상태색: 운행(파랑)·대기(노랑)·완료/수익(초록)·경고(빨강). 운행 상황을 알릴 때만 쓴다. 브랜드 주황은 상태를 뜻하지 않는다 */
object IeoStatus {
    val colors: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}
