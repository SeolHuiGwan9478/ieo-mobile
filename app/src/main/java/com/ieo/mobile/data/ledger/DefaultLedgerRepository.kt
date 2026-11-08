package com.ieo.mobile.data.ledger

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.ledger.DailySummary
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerRepository
import com.ieo.mobile.domain.ledger.ManualEntry
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.ledger.Receivables
import java.time.LocalDate

class DefaultLedgerRepository(
    private val api: LedgerApi,
    private val caller: ApiCaller,
) : LedgerRepository {

    override suspend fun today(): ApiResult<DailySummary> =
        caller.call { api.daily() }.mapResponse { it.toDomain() }

    override suspend fun receivables(): ApiResult<Receivables> =
        caller.call { api.receivables() }.mapResponse { it.toDomain() }

    override suspend fun entries(from: LocalDate, to: LocalDate): ApiResult<List<LedgerEntry>> =
        caller.call { api.entries(from.toString(), to.toString()) }.mapResponse { list -> list.map(LedgerEntryResponse::toDomain) }

    override suspend fun setPaid(entryId: Long, paid: Boolean): ApiResult<LedgerEntry> =
        caller.call { if (paid) api.markPaid(entryId) else api.unmarkPaid(entryId) }.mapResponse { it.toDomain() }

    override suspend fun setExtraIncomePaid(extraIncomeId: Long, paid: Boolean): ApiResult<Unit> =
        caller.callOptional {
            if (paid) api.markExtraIncomePaid(extraIncomeId) else api.unmarkExtraIncomePaid(extraIncomeId)
        }.mapResponse { }

    override suspend fun update(entry: LedgerEntry, fare: Long, paymentType: PaymentType): ApiResult<LedgerEntry> {
        // 서버는 지급 예정일도 함께 받는다. 고치지 않았으면 지금 값을, 없으면 운행일을 보낸다
        val paymentDate = entry.expectedPaymentDate ?: entry.workDate
        val body = UpdateLedgerEntryRequest(fare, paymentType.name, paymentDate.toString())
        return caller.call { api.update(entry.id, body) }.mapResponse { it.toDomain() }
    }

    override suspend fun addManual(entry: ManualEntry): ApiResult<LedgerEntry> =
        caller.call { api.addManual(CreateLedgerEntryRequest.from(entry)) }.mapResponse { it.toDomain() }
}
