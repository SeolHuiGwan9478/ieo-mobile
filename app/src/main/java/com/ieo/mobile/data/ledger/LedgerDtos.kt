package com.ieo.mobile.data.ledger

import com.ieo.mobile.domain.ledger.DailySummary
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.ManualEntry
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.ledger.Receivables
import com.ieo.mobile.domain.trip.StopType
import java.time.LocalDate
import kotlinx.serialization.Serializable

// 서버 domain/ledger/dto와 같은 모양. 쓰지 않는 필드는 뺐다

@Serializable
data class DailySummaryResponse(val date: String, val tripCount: Int, val netProfit: Long) {
    fun toDomain() = DailySummary(date = LocalDate.parse(date), tripCount = tripCount, netProfit = netProfit)
}

@Serializable
data class ReceivablesResponse(
    val fareTotal: Long = 0,
    val overdueFareTotal: Long = 0,
    val waitFeeTotal: Long = 0,
    val entries: List<LedgerEntryResponse>,
) {
    fun toDomain() = Receivables(
        fareTotal = fareTotal,
        overdueFareTotal = overdueFareTotal,
        waitFeeTotal = waitFeeTotal,
        entries = entries.map(LedgerEntryResponse::toDomain),
    )
}

@Serializable
data class LedgerEntryResponse(
    val id: Long,
    val auto: Boolean = false,
    val workDate: String,
    val description: String,
    val fare: Long,
    val paymentType: String,
    val brokerName: String? = null,
    val distanceKm: Int? = null,
    val waitMinutes: Long? = null,
    val expectedPaymentDate: String? = null,
    val paid: Boolean = false,
    val overdue: Boolean = false,
    val extraIncomes: List<ExtraIncomeResponse> = emptyList(),
    val taxInvoiceIssuedAt: String? = null,
) {
    fun toDomain() = LedgerEntry(
        id = id,
        auto = auto,
        workDate = LocalDate.parse(workDate),
        description = description,
        fare = fare,
        paymentType = PaymentType.valueOf(paymentType),
        brokerName = brokerName,
        distanceKm = distanceKm,
        waitMinutes = waitMinutes,
        expectedPaymentDate = expectedPaymentDate?.let(LocalDate::parse),
        paid = paid,
        overdue = overdue,
        extraIncomes = extraIncomes.map(ExtraIncomeResponse::toDomain),
        taxInvoiceIssuedAt = taxInvoiceIssuedAt?.let(java.time.Instant::parse),
    )
}

@Serializable
data class TaxInvoiceResponse(
    val ledgerEntryId: Long,
    val issued: Boolean,
    val writeDate: String,
    val supplier: Party,
    val buyer: Party,
    val item: String,
    val fareAmount: Long,
    val waitFeeAmount: Long,
    val supplyAmount: Long,
    val vat: Long,
    val totalAmount: Long,
    val missing: List<String> = emptyList(),
    val provider: String? = null,
    val approvalNumber: String? = null,
) {
    @Serializable
    data class Party(val businessNumber: String? = null, val name: String? = null, val representative: String? = null)

    fun toDomain() = com.ieo.mobile.domain.ledger.TaxInvoice(
        entryId = ledgerEntryId,
        issued = issued,
        writeDate = LocalDate.parse(writeDate),
        supplier = com.ieo.mobile.domain.ledger.InvoiceParty(supplier.businessNumber, supplier.name, supplier.representative),
        buyer = com.ieo.mobile.domain.ledger.InvoiceParty(buyer.businessNumber, buyer.name, buyer.representative),
        item = item,
        fareAmount = fareAmount,
        waitFeeAmount = waitFeeAmount,
        supplyAmount = supplyAmount,
        vat = vat,
        totalAmount = totalAmount,
        missing = missing,
        demo = provider == null || provider == "DEMO",
        approvalNumber = approvalNumber,
    )
}

@Serializable
data class ExtraIncomeResponse(
    val id: Long,
    val stopType: String,
    val siteName: String,
    val amount: Long,
    val paid: Boolean = false,
) {
    fun toDomain() = ExtraIncome(id = id, stopType = StopType.valueOf(stopType), siteName = siteName, amount = amount, paid = paid)
}

@Serializable
data class UpdateLedgerEntryRequest(val fare: Long, val paymentType: String, val expectedPaymentDate: String, val distanceKm: Int?)

@Serializable
data class CreateLedgerEntryRequest(
    val workDate: String,
    val description: String,
    val fare: Long,
    val paymentType: String,
    val brokerName: String? = null,
    val distanceKm: Int? = null,
) {
    companion object {
        fun from(entry: ManualEntry) = CreateLedgerEntryRequest(
            workDate = entry.workDate.toString(),
            description = entry.description,
            fare = entry.fare,
            paymentType = entry.paymentType.name,
            brokerName = entry.brokerName,
            distanceKm = entry.distanceKm,
        )
    }
}
