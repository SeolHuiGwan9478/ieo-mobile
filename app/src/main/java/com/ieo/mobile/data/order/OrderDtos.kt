package com.ieo.mobile.data.order

import com.ieo.mobile.domain.order.BrokerMetrics
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.order.DispatchRequest
import com.ieo.mobile.domain.order.DispatchRequestStatus
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderProfit
import com.ieo.mobile.domain.order.OrderSite
import java.time.Instant
import kotlinx.serialization.Serializable

// 서버 domain/dispatch/dto와 같은 모양. 쓰지 않는 필드는 뺐다

@Serializable
data class OrderResponse(
    val id: Long,
    val brokerName: String,
    val pickup: OrderSiteResponse,
    val dropoff: OrderSiteResponse,
    val pickupFrom: String,
    val pickupUntil: String,
    val dropoffBy: String,
    val tonnageLabel: String,
    val bodyTypeLabel: String,
    val cargoCategory: String? = null,
    val cargoType: String? = null,
    val weightKg: Int = 0,
    val loadingMethodLabel: String? = null,
    val fare: Long,
    val paymentTypeLabel: String,
    val expectedPaymentDays: Int = 0,
    val distanceM: Long? = null,
    val netProfit: Long? = null,
    val myRequest: DispatchRequestResponse? = null,
    val brokerMetrics: BrokerMetricsResponse? = null,
) {
    fun toDomain() = Order(
        id = id,
        brokerName = brokerName,
        pickup = pickup.toDomain(),
        dropoff = dropoff.toDomain(),
        pickupFrom = Instant.parse(pickupFrom),
        pickupUntil = Instant.parse(pickupUntil),
        dropoffBy = Instant.parse(dropoffBy),
        tonnageLabel = tonnageLabel,
        bodyTypeLabel = bodyTypeLabel,
        cargoType = cargoType?.takeIf { it.isNotBlank() },
        weightKg = weightKg,
        loadingMethodLabel = loadingMethodLabel,
        fare = fare,
        paymentTypeLabel = paymentTypeLabel,
        expectedPaymentDays = expectedPaymentDays,
        distanceMeters = distanceM,
        netProfit = netProfit,
        myRequest = myRequest?.toDomain(),
        cargoCategory = cargoCategory?.let(CargoCategory::fromName),
        brokerMetrics = brokerMetrics?.toDomain(),
    )
}

@Serializable
data class ProfitEstimateResponse(val netProfit: Long, val fuelCost: Long, val tollFare: Long? = null, val emptyMeters: Long) {
    fun toDomain() = OrderProfit(netProfit, fuelCost, tollFare, emptyMeters)
}

@Serializable
data class BrokerMetricsResponse(
    val published: Boolean = false,
    val avgPaymentDays: Int? = null,
    val latePaymentPercent: Int? = null,
    val waitFeePaidPercent: Int? = null,
    val avgWaitMinutes: Int? = null,
) {
    fun toDomain() = BrokerMetrics(published, avgPaymentDays, latePaymentPercent, waitFeePaidPercent, avgWaitMinutes)
}

@Serializable
data class OrderSiteResponse(val id: Long? = null, val name: String, val address: String, val lat: Double, val lng: Double) {
    fun toDomain() = OrderSite(name = name, address = address, latitude = lat, longitude = lng, siteId = id)
}

@Serializable
data class DispatchRequestResponse(val id: Long, val orderId: Long, val status: String, val expiresAt: String) {
    fun toDomain() = DispatchRequest(
        id = id,
        orderId = orderId,
        status = DispatchRequestStatus.valueOf(status),
        expiresAt = Instant.parse(expiresAt),
    )
}

@Serializable
data class CreateDispatchRequestRequest(val vehicleId: Long? = null)

@Serializable
data class CreateOrderReportRequest(val type: String, val memo: String? = null)
