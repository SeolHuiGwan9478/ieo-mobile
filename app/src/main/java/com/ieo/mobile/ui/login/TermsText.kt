package com.ieo.mobile.ui.login

import com.ieo.mobile.domain.auth.Consent

/** 약관 상세에 보여 줄 내용 */
data class TermsSection(val heading: String, val body: String)

data class TermsDetail(val summary: String, val sections: List<TermsSection>)

/** TODO: 출시 전에 법률 검토를 거친 약관 전문으로 바꾼다. 위치 정보만 시안 문구가 있고 나머지는 요약뿐이다 */
fun termsDetail(consent: Consent): TermsDetail = when (consent) {
    Consent.LOCATION -> TermsDetail(
        summary = "운행 중일 때만 차 위치를 사용해요. 주선사에는 따로 동의해야 보내요.",
        sections = listOf(
            TermsSection("1. 모으는 정보", "차량 위치(GPS), 시각, 이동 속도"),
            TermsSection("2. 쓰는 곳", "상차지·하차지 도착 확인, 대기 시간 기록, 근처 오더 추천, 도착 예정 시각 계산"),
            TermsSection("3. 보관 기간", "운행이 끝나고 1년. 대기 증빙에 쓴 기록은 정산이 끝날 때까지"),
            TermsSection("4. 동의하지 않으면", "차주로 가입할 수 없어요."),
        ),
    )
    else -> TermsDetail(
        summary = consent.hint ?: "${consent.label}에 대한 내용이에요.",
        sections = listOf(TermsSection("전문", "약관 전문은 출시 전에 넣을 예정이에요.")),
    )
}
