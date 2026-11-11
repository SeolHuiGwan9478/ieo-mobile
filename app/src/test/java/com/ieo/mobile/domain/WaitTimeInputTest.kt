package com.ieo.mobile.domain

import com.ieo.mobile.domain.proof.WaitTimeInput
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 대기 시각 고치기 입력 (WAIT-006). 시각은 한국 시간 */
class WaitTimeInputTest {

    /** 2026-11-08 09:00 (한국) 도착 */
    private val base = Instant.parse("2026-11-08T00:00:00Z")
    private val now = Instant.parse("2026-11-09T03:00:00Z")

    @Test
    fun 도착한_날_기준으로_바꾸고_비운_칸은_null() {
        val times = WaitTimeInput.resolve(base, "09:10", "10:30", "", now).getOrThrow()
        assertEquals(Instant.parse("2026-11-08T00:10:00Z"), times.arrivedAt)
        assertEquals(Instant.parse("2026-11-08T01:30:00Z"), times.workStartedAt)
        assertNull(times.departedAt)
    }

    @Test
    fun 앞_시각보다_이르면_다음_날로_본다() {
        val times = WaitTimeInput.resolve(base, "22:00", "01:30", "02:10", now).getOrThrow()
        assertEquals(Instant.parse("2026-11-08T16:30:00Z"), times.workStartedAt)
        assertEquals(Instant.parse("2026-11-08T17:10:00Z"), times.departedAt)
    }

    @Test
    fun 형식이_틀리거나_도착이_비거나_지금보다_늦으면_실패() {
        assertTrue(WaitTimeInput.resolve(base, "9시", "", "", now).isFailure)
        assertTrue(WaitTimeInput.resolve(base, "", "10:00", "", now).isFailure)
        assertTrue(WaitTimeInput.resolve(base, "09:00", "", "", Instant.parse("2026-11-07T00:00:00Z")).isFailure)
    }
}
