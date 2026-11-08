package com.ieo.mobile.domain.order

import com.ieo.mobile.core.network.ApiResult

interface OrderRepository {

    /**
     * 내 차량으로 맡을 수 있는 모집 중 오더 (상차가 빠른 순), 최대 [limit]건.
     * [near]를 주면 그곳에서 상차지까지 50km 안만 주고, 오더마다 거리를 붙인다 (근처 오더)
     */
    suspend fun openOrders(limit: Int, near: GeoPoint? = null): ApiResult<List<Order>>

    /** 배차 요청 (기본 차량으로). 동시에 3건까지 */
    suspend fun request(orderId: Long): ApiResult<DispatchRequest>

    /** 확정 전 요청 철회 */
    suspend fun withdraw(requestId: Long): ApiResult<DispatchRequest>

    suspend fun report(orderId: Long, type: ReportType): ApiResult<Unit>
}
