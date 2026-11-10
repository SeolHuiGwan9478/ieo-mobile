package com.ieo.mobile

import com.ieo.mobile.domain.vehicle.PlateNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateNumberTest {

    @Test
    fun `여러 형식의 실제 번호를 받는다`() {
        listOf("12가3456", "123가4567", "경기82사1234", "서울90바5678", "99하9999", "45배1234", "12 가 3456", "경기82-사1234")
            .forEach { assertTrue(it, PlateNumber.isValid(it)) }
    }

    @Test
    fun `용도 글자가 아니거나 자리가 틀리면 막는다`() {
        listOf("12각3456", "12김3456", "1가3456", "1234가5678", "12가345", "경기123가4567", "뉴욕82사1234", "가3456", "")
            .forEach { assertFalse(it, PlateNumber.isValid(it)) }
    }

    @Test
    fun `다 쓰기 전에는 안내하지 않는다`() {
        assertNull(PlateNumber.hint("12가"))
        assertNull(PlateNumber.hint("12가3456"))
        assertEquals("번호를 확인해 주세요 (예: 12가3456, 경기82사1234)", PlateNumber.hint("12김3456"))
    }
}
