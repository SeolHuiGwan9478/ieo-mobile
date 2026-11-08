package com.ieo.mobile.domain.ledger

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.PaymentType
import java.time.LocalDate

interface LedgerRepository {

    /** 오늘(한국 날짜) 요약 */
    suspend fun today(): ApiResult<DailySummary>

    suspend fun receivables(): ApiResult<Receivables>

    /** [from]~[to] (둘 다 포함) 장부, 최근 날짜부터 */
    suspend fun entries(from: LocalDate, to: LocalDate): ApiResult<List<LedgerEntry>>

    /** 운임을 받았음 / 아직 못 받음으로 되돌림 */
    suspend fun setPaid(entryId: Long, paid: Boolean): ApiResult<LedgerEntry>

    /** 대기비를 받았음 / 아직 못 받음으로 되돌림 */
    suspend fun setExtraIncomePaid(extraIncomeId: Long, paid: Boolean): ApiResult<Unit>

    /** 운임·결제 방식 고치기 (LEDG-003). 지급 예정일은 그대로 둔다 */
    suspend fun update(entry: LedgerEntry, fare: Long, paymentType: PaymentType): ApiResult<LedgerEntry>

    /** Ieo로 등록하지 않은 운행 직접 추가 (LEDG-004). 지급 예정일은 서버가 결제 방식으로 정한다 */
    suspend fun addManual(entry: ManualEntry): ApiResult<LedgerEntry>
}

data class ManualEntry(
    val workDate: LocalDate,
    val description: String,
    val fare: Long,
    val paymentType: PaymentType,
    val brokerName: String?,
)
