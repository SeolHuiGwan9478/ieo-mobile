package com.ieo.mobile.ui.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentsTextTest {

    @Test
    fun 받침에_따라_을_를을_붙인다() {
        assertEquals("사업자등록증을", objectOf("사업자등록증"))
        assertEquals("적재물배상책임보험증서를", objectOf("적재물배상책임보험증서"))
        assertEquals("위·수탁(지입) 계약서를", objectOf("위·수탁(지입) 계약서"))
    }

    @Test
    fun 사업자번호와_개업일은_숫자만_받아_하이픈을_붙인다() {
        assertEquals("2020-01", BusinessFields.formatDate("202001"))
        assertEquals("2020-01-31", BusinessFields.openedOn("20200131"))
        assertNull(BusinessFields.openedOn("20201331"))
        assertNull(BusinessFields.openedOn("2020013"))
        assertNull(BusinessFields.openedOn("29991231"))
    }

    @Test
    fun 하이픈은_화면에만_그리고_커서_위치를_맞춘다() {
        val number = HyphenAt(3, 5)
        assertEquals("123-45-67890", number.format("1234567890"))
        assertEquals("123", number.format("123"))
        assertEquals("123-4", number.format("1234"))
        // 끝에 커서: 숫자 10개 뒤 = 화면 12자 뒤
        assertEquals(12, number.toTransformed(10, 10))
        assertEquals(10, number.toOriginal(12, 10))
        // 하이픈 바로 뒤 화면 4 = 숫자 3
        assertEquals(3, number.toOriginal(4, 10))
        assertEquals(4, number.toTransformed(3, 10))
        assertEquals(3, number.toTransformed(3, 3))
    }
}
