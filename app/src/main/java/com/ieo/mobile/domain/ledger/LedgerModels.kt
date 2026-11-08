package com.ieo.mobile.domain.ledger

import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.StopType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 하루 요약 (서버 /api/ledger/daily). 순수익 = 운임 + 대기비 − 경비 */
data class DailySummary(
    val date: LocalDate,
    val tripCount: Int,
    val netProfit: Long,
)

/**
 * 장부 한 줄. 운행이 끝나면 자동으로 생기고([auto]), 차주가 직접 넣을 수도 있다.
 * 운임·결제 방식·주선사는 그때의 값을 그대로 남긴다 (나중에 운행이 바뀌어도 장부는 그대로).
 */
data class LedgerEntry(
    val id: Long,
    val auto: Boolean,
    val workDate: LocalDate,
    /** "평택 포승 → 대구 성서" */
    val description: String,
    val fare: Long,
    val paymentType: PaymentType,
    val brokerName: String?,
    val distanceKm: Int?,
    /** 상차·하차 대기 합계 (자동 줄만) */
    val waitMinutes: Long?,
    val expectedPaymentDate: LocalDate?,
    val paid: Boolean,
    /** 지급 예정일이 지났는데 받지 못함 (서버가 판단) */
    val overdue: Boolean,
    /** 대기비 */
    val extraIncomes: List<ExtraIncome>,
) {
    /** 운임 + 대기비 */
    val total: Long get() = fare + extraIncomes.sumOf { it.amount }
}

/** 대기비 한 건. "청구 예정"으로 생기고 받으면 [paid] */
data class ExtraIncome(
    val id: Long,
    val stopType: StopType,
    val siteName: String,
    val amount: Long,
    val paid: Boolean,
)

/** 아직 받지 못한 운임 (서버 /api/ledger/receivables) */
data class Receivables(
    val fareTotal: Long,
    /** 그중 지급 예정일이 지난 운임 */
    val overdueFareTotal: Long,
    /** 아직 받지 못한 대기비 */
    val waitFeeTotal: Long,
    val entries: List<LedgerEntry>,
) {

    /**
     * 지급일이 지난 운임을 주선사별로 묶어, 가장 오래 밀린 주선사를 고른다. 밀린 게 없으면 null.
     * 홈 화면에는 경고 한 줄만 보여 준다.
     */
    fun mostOverdue(today: LocalDate): OverdueBroker? = entries
        .filter { it.overdue && it.expectedPaymentDate != null }
        .groupBy { it.brokerName ?: UNKNOWN_BROKER }
        .map { (broker, owed) ->
            val oldest = owed.minOf { it.expectedPaymentDate!! }
            OverdueBroker(broker, owed.sumOf { it.fare }, ChronoUnit.DAYS.between(oldest, today).toInt())
        }
        .maxByOrNull { it.overdueDays }

    private companion object {
        const val UNKNOWN_BROKER = "주선사 미상"
    }
}

data class OverdueBroker(val brokerName: String, val amount: Long, val overdueDays: Int)
