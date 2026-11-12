package com.ieo.mobile.data

import com.ieo.mobile.data.ledger.TaxInvoiceResponse
import com.ieo.mobile.data.plan.DriverPlanResponse
import com.ieo.mobile.domain.plan.PlanCode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서버가 보내는 JSON 그대로 읽히는지 (직렬화 표시가 빠지면 앱이 요청 때 멈춘다) */
class PlanAndInvoiceDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `내 플랜 응답을 읽는다`() {
        val body = """{"plan":"PRO","planLabel":"프로","price":9900,"trial":true,"trialAvailable":false,"renews":false,
            "paidUntil":"2026-12-12","daysLeft":30,"features":["LEDGER_HISTORY"],
            "usage":{"waitProofsSent":1,"waitProofLimit":null,"taxInvoicesThisMonth":2,"taxInvoiceLimit":null},
            "plans":[{"code":"BASIC","label":"베이직","price":0,"highlights":["a","b","c"]}],"demoPayment":true}"""
        val plan = json.decodeFromString<DriverPlanResponse>(body).toDomain()
        assertEquals(PlanCode.PRO, plan.plan)
        assertEquals(30, plan.daysLeft)
        assertEquals(null, plan.usage.waitProofLimit)
        assertTrue(plan.trial)
    }

    @Test
    fun `세금계산서 초안 응답을 읽는다`() {
        val body = """{"ledgerEntryId":8,"issued":false,"writeDate":"2026-11-10",
            "supplier":{"businessNumber":null,"name":null,"representative":null},
            "buyer":{"businessNumber":"1234567891","name":"한빛로지스","representative":"이대표"},
            "item":"운송료 이천 → 부산","fareAmount":450000,"waitFeeAmount":30000,"supplyAmount":480000,"vat":48000,"totalAmount":528000,
            "missing":["내 사업자 정보"],"provider":null,"approvalNumber":null,"issuedAt":null}"""
        val invoice = json.decodeFromString<TaxInvoiceResponse>(body).toDomain()
        assertEquals(528_000, invoice.totalAmount)
        assertEquals(listOf("내 사업자 정보"), invoice.missing)
        assertTrue(invoice.demo)
    }
}
