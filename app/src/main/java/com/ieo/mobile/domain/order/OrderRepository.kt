package com.ieo.mobile.domain.order

import com.ieo.mobile.core.network.ApiResult

/** 오더 목록 거르기 (DSP-010). 서버가 거른다 */
data class OrderSearch(val minFare: Long? = null, val prepaidOnly: Boolean = false)

interface OrderRepository {

    /**
     * 내 차량으로 맡을 수 있는 모집 중 오더 (상차가 빠른 순), 최대 [limit]건.
     * [near]를 주면 그곳에서 상차지까지 50km 안만 주고, 오더마다 거리를 붙인다 (근처 오더)
     */
    suspend fun openOrders(limit: Int, near: GeoPoint? = null, search: OrderSearch = OrderSearch()): ApiResult<List<Order>>

    /** [from]에서 이 오더를 맡을 때의 실수익 내역 */
    suspend fun profit(orderId: Long, from: GeoPoint): ApiResult<OrderProfit>

    /** 배차 요청 (기본 차량으로). 동시에 3건까지 */
    suspend fun request(orderId: Long): ApiResult<DispatchRequest>

    /** 확정 전 요청 철회 */
    suspend fun withdraw(requestId: Long): ApiResult<DispatchRequest>

    suspend fun report(orderId: Long, type: ReportType): ApiResult<Unit>
}
