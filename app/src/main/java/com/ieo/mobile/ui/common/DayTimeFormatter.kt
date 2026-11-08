package com.ieo.mobile.ui.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 화면의 날짜·시각 표기를 한국 시간으로 만든다: "오늘" "15:00".
 * 서버는 UTC로 주므로 화면에 쓰는 날짜 계산은 모두 여기를 거친다. [clock]을 고정하면 테스트에서 오늘을 정할 수 있다.
 */
class DayTimeFormatter(private val clock: Clock = Clock.system(KOREA)) {

    fun today(): LocalDate = clock.instant().atZone(KOREA).toLocalDate()

    /** "오늘", "내일", "어제", 그 밖은 "11월 5일" */
    fun day(at: Instant): String {
        val day = at.atZone(KOREA).toLocalDate()
        val today = today()
        return when (day) {
            today -> "오늘"
            today.plusDays(1) -> "내일"
            today.minusDays(1) -> "어제"
            else -> day.format(DAY_FORMAT)
        }
    }

    /** "15:00" */
    fun time(at: Instant): String = at.atZone(KOREA).format(TIME_FORMAT)

    /** "11월 3일 (화)" */
    fun date(day: LocalDate): String = day.format(DATE_WITH_WEEKDAY)

    /** "11월 3일" */
    fun shortDate(day: LocalDate): String = day.format(DAY_FORMAT)

    /** "오늘 17:00", "11월 5일 06:00" */
    fun dayTime(at: Instant): String = "${day(at)} ${time(at)}"

    /** 지금부터 [seconds]초 뒤의 시각 "15:10" */
    fun timeAfter(seconds: Long): String = time(clock.instant().plusSeconds(seconds))

    companion object {
        val KOREA: ZoneId = ZoneId.of("Asia/Seoul")
        private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DATE_WITH_WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN)
    }
}
