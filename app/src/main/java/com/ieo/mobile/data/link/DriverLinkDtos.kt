package com.ieo.mobile.data.link

import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.domain.link.DriverLinkStatus
import java.time.Instant
import kotlinx.serialization.Serializable

// 서버 domain/broker/dto/DriverLinkResponse와 같은 모양. 쓰지 않는 필드는 뺐다

@Serializable
data class DriverLinkResponse(
    val id: Long,
    val brokerName: String,
    val status: String,
    val expiresAt: String? = null,
    val linkedAt: String? = null,
    val brokerMetrics: com.ieo.mobile.data.order.BrokerMetricsResponse? = null,
    val tripsTogether: Long = 0,
) {
    fun toDomain() = DriverLink(
        id = id,
        brokerName = brokerName,
        status = DriverLinkStatus.valueOf(status),
        expiresAt = expiresAt?.let(Instant::parse),
        linkedAt = linkedAt?.let(Instant::parse),
        brokerMetrics = brokerMetrics?.toDomain(),
        tripsTogether = tripsTogether,
    )
}
