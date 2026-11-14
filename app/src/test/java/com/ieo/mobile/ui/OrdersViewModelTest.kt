package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.BrokerMetrics
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.order.DispatchRequest
import com.ieo.mobile.domain.order.DispatchRequestStatus
import com.ieo.mobile.domain.order.GeoPoint
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderProfit
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.order.OrderSite
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.order.DayFilter
import com.ieo.mobile.ui.order.MinFare
import com.ieo.mobile.ui.order.OrderConditions
import com.ieo.mobile.ui.order.OrderRadius
import com.ieo.mobile.ui.order.PaymentFilter
import com.ieo.mobile.ui.order.updatedLabel
import com.ieo.mobile.ui.order.OrderTexts
import com.ieo.mobile.ui.order.OrdersViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** 한국 2026-11-08 12:00 */
    private val now = Instant.parse("2026-11-08T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val today = now.plusSeconds(3_600)
    private val tomorrow = now.plusSeconds(86_400)

    private fun order(
        id: Long,
        at: Instant = today,
        fare: Long = 300_000,
        payment: String = "인수증",
        request: DispatchRequest? = null,
        cargo: CargoCategory? = null,
        metrics: BrokerMetrics? = null,
    ) = Order(
        id, "한빛", OrderSite("칠곡", "경북", 35.9, 128.5), OrderSite("부산", "강서", 35.1, 128.8), at, at.plusSeconds(7_200), at.plusSeconds(36_000),
        "5톤", "카고", "파렛트", 4_200, "지게차", fare, payment, 15, null, request, cargo, metrics,
    )

    private inner class Fake : OrderRepository {
        var list = listOf(order(1, today, 300_000), order(2, tomorrow, 500_000, "선불"))
        var nearAsked: GeoPoint? = null
        var result: ApiResult<DispatchRequest> = ApiResult.Success(DispatchRequest(9, 1, DispatchRequestStatus.PENDING, tomorrow))
        val calls = mutableListOf<String>()
        var searched: com.ieo.mobile.domain.order.OrderSearch? = null
        override suspend fun openOrders(limit: Int, near: GeoPoint?, search: com.ieo.mobile.domain.order.OrderSearch): ApiResult<List<Order>> {
            nearAsked = near
            searched = search
            // 결제 방식과 운임은 서버가 거른다
            return ApiResult.Success(list.filter {
                (search.paymentType == null || (search.paymentType == "PREPAID") == (it.paymentTypeLabel == "선불")) &&
                    (search.minFare == null || it.fare >= search.minFare)
            })
        }
        var profit: ApiResult<OrderProfit> = ApiResult.Success(OrderProfit(200_000, 50_000, 7_800, 6_000))
        var profitAsked = 0
        override suspend fun profit(orderId: Long, from: GeoPoint): ApiResult<OrderProfit> {
            profitAsked++
            return profit
        }
        override suspend fun request(orderId: Long): ApiResult<DispatchRequest> {
            calls += "request$orderId"
            return result
        }
        override suspend fun withdraw(requestId: Long): ApiResult<DispatchRequest> {
            calls += "withdraw$requestId"
            return result
        }
        override suspend fun report(orderId: Long, type: ReportType): ApiResult<Unit> {
            calls += "report$orderId:$type"
            return ApiResult.Success(Unit)
        }
    }

    private val here = GeoPoint(37.0, 127.0)

    private fun TestVm(fake: Fake, location: GeoPoint? = here) = OrdersViewModel(fake, { location }, clock)

    @Test
    fun `위치를 알면 근처로 찾고 전국을 고르면 위치 없이 찾는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        assertEquals(here, fake.nearAsked)
        assertTrue(vm.state.value.nearApplied)
        vm.openConditions()
        vm.editDraft { it.copy(radius = OrderRadius.NATIONWIDE) }
        vm.applyConditions()
        advanceUntilIdle()
        assertNull(fake.nearAsked)
        assertEquals(false, vm.state.value.locationMissing)
    }

    @Test
    fun `오더를 열면 실수익 내역을 받고 닫으면 지운다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        advanceUntilIdle()
        assertEquals(200_000L, vm.state.value.profit?.netProfit)
        vm.close()
        assertNull(vm.state.value.profit)
    }

    @Test
    fun `위치를 모르거나 실패하면 실수익 내역 없이 연다`() = runTest(main.dispatcher) {
        val unknown = Fake()
        val noLocation = TestVm(unknown, location = null).apply { refresh() }
        advanceUntilIdle()
        noLocation.open(1)
        advanceUntilIdle()
        assertEquals(0, unknown.profitAsked)
        assertNull(noLocation.state.value.profit)

        val failing = Fake().apply { profit = ApiResult.Failure(ApiError.Network) }
        val vm = TestVm(failing).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        advanceUntilIdle()
        assertEquals(1L, vm.state.value.selectedId)
        assertNull(vm.state.value.profit)
    }

    @Test
    fun `목록의 실수익은 만 원 단위 어림으로 적고 없으면 뺀다`() {
        val texts = OrderTexts()
        assertEquals("실수익 약 28만 원", texts.netProfit(order(1).copy(netProfit = 275_400)))
        assertNull(texts.netProfit(order(1)))
    }

    @Test
    fun `위치를 모르면 전체에서 찾는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake, location = null).apply { refresh() }
        advanceUntilIdle()
        assertNull(fake.nearAsked)
        assertEquals(false, vm.state.value.nearApplied)
        assertTrue(vm.state.value.locationMissing)
    }

    @Test
    fun `조건 창에서 고른 뒤 적용해야 목록이 바뀌고 건수를 미리 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.openConditions()
        vm.editDraft { it.copy(payment = PaymentFilter.PREPAID) }
        advanceUntilIdle()
        assertEquals(1, vm.state.value.draftCount)
        // 아직 적용 전이라 목록은 그대로
        assertEquals(listOf(1L, 2L), vm.visible(vm.state.value).map { it.id })
        vm.applyConditions()
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.visible(vm.state.value).map { it.id })
        assertEquals("PREPAID", fake.searched?.paymentType)
        assertEquals(1, vm.state.value.conditions.changedCount)
        assertEquals(listOf("내 주변 50km", "선불"), vm.state.value.conditions.summary)
    }

    @Test
    fun `날짜와 제외할 화물은 받은 목록에서 거르고 운임 순으로 바꾼다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { list = listOf(order(1, today, 300_000), order(2, tomorrow, 500_000, "선불"), order(3, today, 400_000, cargo = CargoCategory.MOVING)) }
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.toggleSort()
        assertEquals(listOf(2L, 3L, 1L), vm.visible(vm.state.value).map { it.id })
        vm.openConditions()
        vm.editDraft { it.copy(day = DayFilter.TODAY, excluded = setOf(CargoCategory.MOVING)) }
        vm.applyConditions()
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.visible(vm.state.value).map { it.id })
        assertEquals(listOf("내 주변 50km", "오늘 상차", "이사 제외"), vm.state.value.conditions.summary)
    }

    @Test
    fun `제외할 화물은 배차 추천 설정에서 읽고 바꾸면 거기에 저장한다`() = runTest(main.dispatcher) {
        val store = object : com.ieo.mobile.domain.recommendation.ExcludedCargoStore {
            var saved: Set<CargoCategory> = setOf(CargoCategory.MOVING)
            override suspend fun load() = saved
            override suspend fun save(excluded: Set<CargoCategory>) { saved = excluded }
        }
        val vm = OrdersViewModel(Fake(), { here }, clock, store).apply { refresh() }
        advanceUntilIdle()
        assertEquals(setOf(CargoCategory.MOVING), vm.state.value.conditions.excluded)

        vm.openConditions()
        vm.editDraft { it.copy(excluded = setOf(CargoCategory.HAZARDOUS)) }
        vm.applyConditions()
        advanceUntilIdle()
        assertEquals(setOf(CargoCategory.HAZARDOUS), store.saved)
        assertEquals(setOf(CargoCategory.HAZARDOUS), vm.state.value.conditions.excluded)
    }

    @Test
    fun `30만 원 이상을 고르면 서버에 최소 운임을 보낸다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.openConditions()
        vm.editDraft { it.copy(minFare = MinFare.OVER_30) }
        vm.applyConditions()
        advanceUntilIdle()
        assertEquals(300_000L, fake.searched?.minFare)
    }

    @Test
    fun `자동 새로고침은 새로 들어온 오더에 표시를 붙이고 상세를 보는 동안에는 미룬다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        assertTrue(vm.state.value.newIds.isEmpty())

        fake.list = fake.list + order(5, today, 450_000)
        vm.open(1)
        vm.autoRefresh()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.orders.size)   // 상세가 열려 있어 아직 안 받았다

        vm.close()
        advanceUntilIdle()
        assertEquals(setOf(5L), vm.state.value.newIds)
    }

    @Test
    fun `업데이트 시간은 1분이 안 되면 방금, 넘으면 분으로 적는다`() {
        assertEquals("방금 업데이트", updatedLabel(now, now.plusSeconds(40)))
        assertEquals("3분 전 업데이트", updatedLabel(now, now.plusSeconds(200)))
    }

    @Test
    fun `배차 요청을 보내고 목록을 새로 받는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        fake.list = listOf(order(1, request = DispatchRequest(9, 1, DispatchRequestStatus.PENDING, tomorrow)))
        vm.request()
        advanceUntilIdle()
        assertEquals(listOf("request1"), fake.calls)
        assertEquals(true, vm.state.value.selected?.isRequestedByMe)
        assertEquals(OrdersViewModel.REQUESTED_MESSAGE, vm.state.value.message)
    }

    @Test
    fun `요청이 실패하면 서버 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Server(429, "DISPATCH_429_LIMIT", "대기 중인 배차 요청은 3건까지 걸어 둘 수 있습니다.")) }
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        vm.request()
        advanceUntilIdle()
        assertEquals("대기 중인 배차 요청은 3건까지 걸어 둘 수 있습니다.", vm.state.value.message)
    }

    @Test
    fun `내 요청을 철회한다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { list = listOf(order(1, request = DispatchRequest(9, 1, DispatchRequestStatus.PENDING, tomorrow))) }
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        vm.withdraw()
        advanceUntilIdle()
        assertEquals(listOf("withdraw9"), fake.calls)
    }

    @Test
    fun `신고하면 사유를 보내고 창을 닫고 다시 신고하지 못하게 한다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TestVm(fake).apply { refresh() }
        advanceUntilIdle()
        vm.open(1)
        vm.startReport()
        vm.report(ReportType.OVERLOAD)
        advanceUntilIdle()
        assertEquals(listOf("report1:OVERLOAD"), fake.calls)
        assertEquals(false, vm.state.value.reporting)
        assertEquals(true, vm.state.value.selected?.reportedByMe)
        assertEquals(OrdersViewModel.REPORTED_MESSAGE, vm.state.value.message)
        vm.clearMessage()
        assertEquals(null, vm.state.value.message)
    }

    @Test
    fun `문구를 만든다`() {
        val texts = OrderTexts(DayTimeFormatter(clock))
        val order = order(1, today, cargo = CargoCategory.COLD, metrics = BrokerMetrics(published = true, avgPaymentDays = 12, latePaymentPercent = 8))
        assertEquals("칠곡 → 부산", texts.route(order))
        assertEquals("상차 오늘 13:00~15:00 · 하차 오늘 23:00까지", texts.schedule(order))
        assertEquals("파렛트 · 4.2톤", texts.cargo(order))
        assertEquals("지게차", texts.loadingTag(order)?.text)
        assertEquals("평균 지급 12일 · 지급 지연 8%", texts.metrics(order.brokerMetrics!!))
        assertEquals(listOf("5톤 카고", "냉동·냉장", "인수증", "지급 평균 12일"), texts.tags(order).map { it.text })
        assertEquals(listOf("5톤 카고", "냉동·냉장", "인수증"), texts.detailTags(order).map { it.text })
        assertEquals("거래 기록 적음", texts.metrics(BrokerMetrics(published = false)))
        // 단골 주선사가 나에게만 보낸 지정 오더는 맨 앞에 "단골" (BRK-011)
        val mine = order.copy(forMeOnly = true)
        assertEquals("단골", texts.tags(mine).first().text)
        assertEquals("단골", texts.detailTags(mine).first().text)
        assertEquals("30만 원", formatWon(300_000))
    }
}
