package com.ieo.mobile.domain.vehicle

/**
 * 자동차 등록번호 검사. 서버(Vehicle.PLATE_NUMBER)와 같은 규칙이다. 예: 12가3456, 123가4567, 경기82사1234
 * - 숫자 2~3자리 + 용도 글자 + 숫자 4자리. 사업용은 지역명 + 숫자 2자리로 시작할 수 있다
 * - 용도 글자: 비사업용 가~마·거~저·고~조·구~주(32자), 사업용 아·바·사·자, 렌터카 하·허·호, 택배 배
 */
object PlateNumber {

    private val PATTERN = Regex(
        "^(?:(?:서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)\\d{2}|\\d{2,3})" +
            "[가나다라마거너더러머버서어저고노도로모보소오조구누두루무부수우주아바사자하허호배]\\d{4}$",
    )

    /** 띄어쓰기와 하이픈을 지운다 */
    fun normalize(input: String): String = input.replace(Regex("[\\s-]"), "")

    fun isValid(input: String): Boolean = PATTERN.matches(normalize(input))

    /** 쓰는 중에는 조용히 있다가, 다 쓴 것 같은데 틀리면 알려 준다 */
    fun hint(input: String): String? = when {
        normalize(input).length < 7 -> null
        isValid(input) -> null
        else -> "번호를 확인해 주세요 (예: 12가3456, 경기82사1234)"
    }
}
