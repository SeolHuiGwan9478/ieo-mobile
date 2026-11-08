package com.ieo.mobile.data.order

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.order.DispatchRequest
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.domain.order.GeoPoint

class DefaultOrderRepository(
    private val api: OrderApi,
    private val caller: ApiCaller,
) : OrderRepository {

    override suspend fun openOrders(limit: Int, near: GeoPoint?): ApiResult<List<Order>> =
        caller.call { api.openOrders(lat = near?.latitude, lng = near?.longitude, size = limit) }.mapResponse { orders -> orders.map(OrderResponse::toDomain) }

    override suspend fun request(orderId: Long): ApiResult<DispatchRequest> =
        caller.call { api.request(orderId) }.mapResponse { it.toDomain() }

    override suspend fun withdraw(requestId: Long): ApiResult<DispatchRequest> =
        caller.call { api.withdraw(requestId) }.mapResponse { it.toDomain() }

    override suspend fun report(orderId: Long, type: ReportType): ApiResult<Unit> =
        caller.callOptional { api.report(orderId, CreateOrderReportRequest(type.name)) }.mapResponse { }
}
