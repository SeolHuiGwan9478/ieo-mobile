package com.ieo.mobile.ui

import com.ieo.mobile.ui.login.LoginStep
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.DeliveryProofStatus
import com.ieo.mobile.domain.proof.ProofLink
import com.ieo.mobile.domain.proof.ProofRepository
import com.ieo.mobile.domain.proof.ReceiptPhoto
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.proof.WaitTimes
import com.ieo.mobile.ui.proof.ProofScreen
import com.ieo.mobile.ui.proof.ProofViewModel
import com.ieo.mobile.domain.site.HeightCheck
import com.ieo.mobile.domain.site.HeightLevel
import com.ieo.mobile.domain.site.SiteAttributeKey
import com.ieo.mobile.domain.site.SiteAttributes
import com.ieo.mobile.domain.site.SiteCard
import com.ieo.mobile.domain.site.SiteNote
import com.ieo.mobile.domain.site.SiteRepository
import com.ieo.mobile.domain.site.SiteStats
import com.ieo.mobile.ui.site.SiteCardScreen
import com.ieo.mobile.ui.site.SiteCardViewModel
import androidx.compose.foundation.layout.Box
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationRepository
import com.ieo.mobile.domain.notification.NotificationSeenStore
import com.ieo.mobile.domain.notification.NotificationStreamEvent
import com.ieo.mobile.domain.notification.NotificationType
import com.ieo.mobile.ui.notification.NotificationBanner
import com.ieo.mobile.ui.notification.NotificationBell
import com.ieo.mobile.ui.notification.NotificationsScreen
import com.ieo.mobile.ui.notification.NotificationsViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.auth.PhoneVerification
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.expense.ExpenseRepository
import com.ieo.mobile.domain.ledger.DailySummary
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerRepository
import com.ieo.mobile.domain.ledger.ManualEntry
import com.ieo.mobile.domain.ledger.Receivables
import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.domain.link.DriverLinkRepository
import com.ieo.mobile.domain.link.DriverLinkStatus
import com.ieo.mobile.domain.order.BrokerMetrics
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.order.DispatchRequest
import com.ieo.mobile.domain.order.DispatchRequestStatus
import com.ieo.mobile.domain.order.GeoPoint
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.order.OrderSite
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.domain.recommendation.RecommendationPreference
import com.ieo.mobile.domain.recommendation.RecommendationRepository
import com.ieo.mobile.domain.trip.CancelReason
import com.ieo.mobile.domain.trip.NewTrip
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.PlaceSearch
import com.ieo.mobile.domain.trip.StopChoice
import com.ieo.mobile.domain.trip.StopType
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.domain.trip.TripStatus
import com.ieo.mobile.domain.trip.TripStop
import com.ieo.mobile.domain.trip.VisitedSite
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleRepository
import com.ieo.mobile.ui.home.HomeScreen
import com.ieo.mobile.ui.login.LoginScreen
import com.ieo.mobile.ui.login.LoginViewModel
import com.ieo.mobile.ui.link.LinksScreen
import com.ieo.mobile.ui.link.LinksViewModel
import com.ieo.mobile.ui.my.MyScreen
import com.ieo.mobile.ui.my.MyViewModel
import com.ieo.mobile.ui.ledger.LedgerOverlay
import com.ieo.mobile.ui.ledger.LedgerScreen
import com.ieo.mobile.ui.ledger.LedgerViewModel
import com.ieo.mobile.ui.order.OrderDetailScreen
import com.ieo.mobile.ui.order.OrdersScreen
import com.ieo.mobile.ui.order.OrdersViewModel
import com.ieo.mobile.ui.recommendation.RecommendationScreen
import com.ieo.mobile.ui.recommendation.RecommendationViewModel
import com.ieo.mobile.ui.theme.IeoTheme
import com.ieo.mobile.ui.trip.CancelTripDialog
import com.ieo.mobile.ui.trip.RegisterTripScreen
import com.ieo.mobile.ui.trip.RegisterTripViewModel
import com.ieo.mobile.ui.trip.TripDetailScreen
import com.ieo.mobile.ui.trip.TripsScreen
import com.ieo.mobile.ui.trip.TripsViewModel
import com.ieo.mobile.ui.vehicle.VehiclesScreen
import com.ieo.mobile.ui.vehicle.VehiclesState
import com.ieo.mobile.ui.vehicle.VehiclesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 폰 없이 휴대폰 크기(390×844)로 화면을 그려 build/screens에 PNG로 남긴다 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-port-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeAuth(val existing: ExistingAccount? = null, val verifyFail: Boolean = false) : AuthRepository {
        override suspend fun requestCode(phone: String) = ApiResult.Success<String?>("381942")
        override suspend fun verify(phone: String, code: String) =
            if (verifyFail) ApiResult.Failure(ApiError.Server(400, "AUTH_400_CODE", "인증번호가 달라요 (4번 남음)"))
            else ApiResult.Success(PhoneVerification("t", existing))
        override suspend fun login(phoneToken: String) = ApiResult.Success(Unit)
        override suspend fun signUp(phoneToken: String, name: String, consents: Set<Consent>, vehicle: NewVehicle?) = ApiResult.Success(Unit)
        override fun finishSignUp() = Unit
    }

    private fun shot(name: String, viewModel: LoginViewModel? = null, content: @Composable () -> Unit = { LoginScreen(viewModel!!) }) {
        compose.setContent {
            IeoTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() } }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    private fun toCode(vm: LoginViewModel, code: String = "381942") {
        vm.start()
        vm.onPhoneChange("01012345678")
        vm.requestCode()
        compose.waitForIdle()
        vm.onCodeChange(code)
    }

    @Test
    fun M01_시작() = shot("M01_start", LoginViewModel(FakeAuth()))

    @Test
    fun V01_번호() {
        val vm = LoginViewModel(FakeAuth())
        vm.start()
        vm.onPhoneChange("0101234567")
        shot("V01_phone", vm)
    }

    @Test
    fun V02_인증번호() {
        val vm = LoginViewModel(FakeAuth())
        toCode(vm, "3819")
        shot("V02_code", vm)
    }

    @Test
    fun V03_틀림() {
        val vm = LoginViewModel(FakeAuth(verifyFail = true))
        toCode(vm)
        vm.verify()
        compose.waitForIdle()
        shot("V03_error", vm)
    }

    @Test
    fun V04_약관() {
        val vm = LoginViewModel(FakeAuth())
        toCode(vm)
        vm.verify()
        compose.waitForIdle()
        Consent.entries.filter { it.required }.forEach(vm::toggleConsent)
        shot("V04_terms", vm)
    }

    @Test
    fun V06_기본정보() {
        val vm = LoginViewModel(FakeAuth())
        toCode(vm)
        vm.verify()
        compose.waitForIdle()
        vm.agreeToTerms()
        vm.onNameChange("김도윤")
        vm.nameDone()
        vm.onPlateChange("경기82사1234")
        shot("V06_info", vm)
    }

    @Test
    fun V08_내차_톤수() {
        val vm = signUpTo(LoginStep.VEHICLE)
        vm.onPlateChange("경기82사1234")
        shot("V08_vehicle", vm)
    }

    @Test
    fun V09_차량종류() {
        val vm = signUpTo(LoginStep.VEHICLE)
        vm.onPlateChange("경기82사1234")
        vm.vehicleDone()
        vm.onBodyType(com.ieo.mobile.domain.vehicle.BodyType.WING)
        shot("V09_body", vm)
    }

    @Test
    fun V10_내차_제원입력() {
        val vm = signUpTo(LoginStep.VEHICLE)
        vm.onPlateChange("경기82사1234")
        vm.onHeight(3.6)
        vm.onDimensions(com.ieo.mobile.domain.vehicle.VehicleDimensions(249, 850, null, 3))
        shot("V10_body_specs", vm)
    }

    private fun signUpTo(step: LoginStep): LoginViewModel {
        val vm = LoginViewModel(FakeAuth())
        toCode(vm)
        compose.waitForIdle()
        vm.agreeToTerms()
        vm.onNameChange("김도윤")
        vm.nameDone()
        return vm
    }

    @Test
    fun V07_완료() {
        val vm = LoginViewModel(FakeAuth())
        toCode(vm)
        vm.verify()
        compose.waitForIdle()
        vm.agreeToTerms()
        vm.onNameChange("김도윤")
        vm.onPlateChange("경기82사1234")
        vm.signUp()
        compose.waitForIdle()
        shot("V07_done", vm)
    }

    /** 로고·문구·버튼이 다 나타난 뒤의 시작 화면 */
    @Test
    fun 시작화면_완성() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            IeoTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { com.ieo.mobile.ui.login.StartScreen({}) } }
        }
        compose.mainClock.advanceTimeBy(4_000)
        compose.onRoot().captureRoboImage("build/screens/M01_start_ready.png")
    }

    private fun carLogin(): com.ieo.mobile.ui.carlogin.CarLoginViewModel =
        com.ieo.mobile.ui.carlogin.CarLoginViewModel(object : com.ieo.mobile.domain.carlogin.CarLoginRepository {
            override suspend fun find(code: String) = ApiResult.Success(
                com.ieo.mobile.domain.carlogin.CarLoginRequest(code, "Pleos 5톤 카고", "경기 평택", Instant.parse("2026-11-10T09:05:00Z"), true))
            override suspend fun approve(code: String) = ApiResult.Success(Unit)
            override suspend fun reject(code: String) = ApiResult.Success(Unit)
        })

    @Test
    fun CL_차량로그인승인() {
        val vm = carLogin()
        shot("CL_ask", content = { com.ieo.mobile.ui.carlogin.CarLoginScreen("KX7P-M4QZ", vm, onClose = {}) })
    }

    private fun accountSettings() = com.ieo.mobile.ui.my.AccountSettingsViewModel(object : com.ieo.mobile.domain.account.AccountSettingsRepository {
        override suspend fun carSessions() = ApiResult.Success(listOf(
            com.ieo.mobile.domain.account.CarSession("a", "Pleos 5톤 카고", Instant.parse("2026-11-10T00:30:00Z")),
            com.ieo.mobile.domain.account.CarSession("b", "Pleos 데모 차량", Instant.parse("2026-11-08T06:10:00Z"))))
        override suspend fun revokeCarSession(id: String) = ApiResult.Success(Unit)
        override suspend fun consents() = ApiResult.Success(listOf(
            com.ieo.mobile.domain.account.ConsentItem("TERMS", "이용약관", true, true),
            com.ieo.mobile.domain.account.ConsentItem("PRIVACY", "개인정보 수집·이용", true, true),
            com.ieo.mobile.domain.account.ConsentItem("LOCATION", "위치 정보 수집", true, true),
            com.ieo.mobile.domain.account.ConsentItem("BROKER_SHARE", "주선사 공유", false, true),
            com.ieo.mobile.domain.account.ConsentItem("VOICE", "음성", false, true),
            com.ieo.mobile.domain.account.ConsentItem("MARKETING", "마케팅", false, false)))
        override suspend fun changeConsent(type: String, agreed: Boolean) = ApiResult.Failure(ApiError.Network)
        override suspend fun business() = ApiResult.Success<com.ieo.mobile.domain.account.BusinessInfo?>(
            com.ieo.mobile.domain.account.BusinessInfo("한길운수", "1234567891", "김도윤"))
        override suspend fun saveBusiness(info: com.ieo.mobile.domain.account.BusinessInfo) = ApiResult.Success<com.ieo.mobile.domain.account.BusinessInfo?>(info)
        override suspend fun clearBusiness() = ApiResult.Success(Unit)
    })

    @Test
    fun AS_로그인된차량() = shot("AS_cars", content = { com.ieo.mobile.ui.my.AccountSettingsScreen(com.ieo.mobile.ui.my.AccountSection.CARS, accountSettings(), onBack = {}) })

    @Test
    fun AS_동의관리() = shot("AS_consents", content = { com.ieo.mobile.ui.my.AccountSettingsScreen(com.ieo.mobile.ui.my.AccountSection.CONSENTS, accountSettings(), onBack = {}) })

    @Test
    fun AS_사업자정보() = shot("AS_business", content = { com.ieo.mobile.ui.my.AccountSettingsScreen(com.ieo.mobile.ui.my.AccountSection.BUSINESS, accountSettings(), onBack = {}) })

    @Test
    fun M11_내정보() {
        val accounts = object : AccountRepository {
            override suspend fun me() = ApiResult.Success(Account("김도윤", "01012345678"))
            override suspend fun rename(name: String) = ApiResult.Success(Account(name, "01012345678"))
            override suspend fun changePhone(phoneToken: String) = ApiResult.Success(Account("김도윤", "01099998888"))
            override suspend fun withdraw() = ApiResult.Success(Unit)
            override fun signOut() = Unit
        }
        val vm = MyViewModel(accounts)
        shot("M11_my", content = { MyScreen(vm, VehiclesState(vehicles = listOf(sample)), 2, 1, "켬 · 30km", onVehicles = {}, onLinks = {}, onRecommendation = {}, documents = documents(com.ieo.mobile.domain.document.DocumentStatus.APPROVED, null, null, null), record = com.ieo.mobile.domain.document.DriverRecord(false, 0, 0)) })
    }

    private val sample = Vehicle(1, "경기82사1234", Tonnage.TON_5, BodyType.CARGO, 3.6, isDefault = true, isLinkedToCar = true)
    private val other = Vehicle(2, "경기80바5678", Tonnage.TON_11, BodyType.WING, null, isDefault = false)

    private fun vehicles(saved: List<Vehicle> = listOf(sample)): VehiclesViewModel = VehiclesViewModel(object : VehicleRepository {
        override suspend fun list() = ApiResult.Success(saved)
        override suspend fun add(vehicle: NewVehicle) = ApiResult.Success(Unit)
        override suspend fun update(vehicleId: Long, tonnage: Tonnage, bodyType: BodyType, heightMeters: Double?, dimensions: com.ieo.mobile.domain.vehicle.VehicleDimensions, plateNumber: String?) = ApiResult.Success(Unit)
        override suspend fun setManualFuelEconomy(vehicleId: Long, kmPerLiter: Double?) = ApiResult.Success(Unit)
    })

    @Test
    fun 내차량() {
        val vm = vehicles()
        shot("VH_edit", content = { VehiclesScreen(vm, onBack = {}) })
    }

    @Test
    fun 내차량_톤수변경() {
        val vm = vehicles()
        compose.setContent {
            IeoTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { VehiclesScreen(vm, onBack = {}) } }
        }
        compose.waitForIdle()
        vm.stepTonnage(1)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/VH_identity.png")
    }

    @Test
    fun 내차량_연비() {
        val vm = vehicles()
        compose.setContent {
            IeoTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { VehiclesScreen(vm, onBack = {}) } }
        }
        compose.waitForIdle()
        vm.onFuelEconomy("5.5")
        compose.onNodeWithText("기름값", substring = true).performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/VH_fuel.png")
    }

    @Test
    fun M06_차량등록() {
        val vm = vehicles(emptyList())
        shot("M06_add", content = { VehiclesScreen(vm, onBack = {}) })
    }

    private fun order(id: Long, route: Pair<String, String>, fare: Long, payment: String, days: Int?, mine: Boolean = false, cargo: CargoCategory? = null) = Order(
        id = id,
        brokerName = "□□로지스틱스",
        pickup = OrderSite(route.first, "경기 화성시 향남읍 발안산단", 37.0, 127.0),
        dropoff = OrderSite(route.second, "경남 창원시 성산구 성주동", 35.2, 128.6),
        pickupFrom = Instant.now().plusSeconds(3 * 3600),
        pickupUntil = Instant.now().plusSeconds(4 * 3600),
        dropoffBy = Instant.now().plusSeconds(11 * 3600),
        tonnageLabel = "5톤",
        bodyTypeLabel = "카고",
        cargoType = "건축 자재",
        weightKg = 4800,
        loadingMethodLabel = "지게차",
        fare = fare,
        paymentTypeLabel = payment,
        expectedPaymentDays = 25,
        distanceMeters = 18_000,
        myRequest = if (mine) DispatchRequest(9, id, DispatchRequestStatus.PENDING, Instant.now().plusSeconds(540)) else null,
        cargoCategory = cargo,
        brokerMetrics = BrokerMetrics(published = days != null, avgPaymentDays = days, latePaymentPercent = days?.let { 8 }, avgWaitMinutes = days?.let { 35 }),
    )

    private fun orders(): OrdersViewModel = OrdersViewModel(object : OrderRepository {
        override suspend fun openOrders(limit: Int, near: GeoPoint?, search: com.ieo.mobile.domain.order.OrderSearch) = ApiResult.Success(listOf(
            order(1, "화성 향남" to "창원 성산", 580_000, "인수증", 25),
            order(2, "평택 포승" to "부산 강서", 550_000, "선불", null),
            order(3, "평택 청북" to "대구 북구", 470_000, "인수증", 12, mine = true),
            order(4, "오산 가장" to "광주 광산", 410_000, "선불", 9, cargo = CargoCategory.MOVING),
        ))
        override suspend fun request(orderId: Long) = ApiResult.Failure(ApiError.Network)
        override suspend fun profit(orderId: Long, from: GeoPoint) = ApiResult.Failure(ApiError.Network)
        override suspend fun withdraw(requestId: Long) = ApiResult.Failure(ApiError.Network)
        override suspend fun report(orderId: Long, type: ReportType) = ApiResult.Success(Unit)
    }, location = { GeoPoint(37.0, 127.0) })

    @Test
    fun M08_오더목록() {
        val vm = orders()
        shot("M08_orders", content = { OrdersScreen(vm, askLocation = false) })
    }

    @Test
    fun M09_오더상세() {
        val vm = orders()
        vm.refresh()
        compose.waitForIdle()
        vm.open(1)
        shot("M09_detail", content = { OrderDetailScreen(vm.state.value.selected!!, vm.state.value, vm) })
    }

    @Test
    fun M09_오더상세_서류심사전() {
        val vm = orders()
        vm.refresh()
        compose.waitForIdle()
        vm.open(1)
        val order = vm.state.value.selected!!
        val limited = order.copy(
            brokerName = "", limited = true, cargoType = "일반",
            pickup = order.pickup.copy(name = "경기 평택시", address = "경기 평택시", siteId = null),
            dropoff = order.dropoff.copy(name = "대구 달서구", address = "대구 달서구", siteId = null),
        )
        shot("M09_detail_limited", content = { OrderDetailScreen(limited, vm.state.value, vm, verified = false) })
    }

    @Test
    fun 오더상세_요청중() {
        val vm = orders()
        vm.refresh()
        compose.waitForIdle()
        vm.open(3)
        shot("M09_requested", content = { OrderDetailScreen(vm.state.value.selected!!, vm.state.value, vm) })
    }

    private fun trip(id: Long, status: TripStatus, manual: Boolean = false) = Trip(
        id, status, 450_000, "인수증", "○○물류", "경기82사1234",
        TripStop("평택 포승", "경기 평택시 포승읍 평택항로 156", Instant.now().plusSeconds(3600)),
        TripStop("대구 성서", "대구 달서구 성서공단로 11", null),
        isManual = manual, createdAt = Instant.now(),
    )

    private val tripRepo = object : TripRepository {
        override suspend fun list() = ApiResult.Success(listOf(trip(1, TripStatus.IN_TRANSIT), trip(2, TripStatus.SCHEDULED, manual = true), trip(3, TripStatus.COMPLETED)))
        override suspend fun visitedSites() = ApiResult.Success(listOf(VisitedSite(1, "평택 포승", "경기 평택시 포승읍"), VisitedSite(2, "대구 성서", "대구 달서구")))
        override suspend fun register(trip: NewTrip) = ApiResult.Success(Unit)
        override suspend fun cancel(tripId: Long, reason: CancelReason?) = ApiResult.Success(Unit)
    }

    private val places = object : PlaceSearch {
        override suspend fun search(query: String) = listOf(StopChoice.New(query, "경기도 화성시 향남읍 발안공단로 1", 37.0, 126.9))
        override suspend fun here() = StopChoice.New("여기", "경기도 김포시 고촌읍", 37.6, 126.7)
    }

    @Test
    fun 배차목록() {
        val vm = TripsViewModel(tripRepo)
        shot("TR_list", content = { TripsScreen(vm, onRegister = {}) })
    }

    @Test
    fun 배차상세() {
        val vm = TripsViewModel(tripRepo)
        compose.waitForIdle()
        vm.open(2)
        shot("TR_detail", content = { TripDetailScreen(vm.state.value.selected!!, vm.state.value, vm) })
    }

    @Test
    fun 배차취소_늦은취소() {
        shot("TR_cancel_late", content = {
            CancelTripDialog(trip(4, TripStatus.SCHEDULED), late = true, onCancel = {}, onDismiss = {})
        })
    }

    @Test
    fun 배차상세_짐실은뒤() {
        val vm = TripsViewModel(tripRepo)
        compose.waitForIdle()
        vm.open(1)
        shot("TR_detail_loaded", content = { TripDetailScreen(vm.state.value.selected!!, vm.state.value, vm) })
    }

    @Test
    fun 배차등록_장소() {
        val vm = RegisterTripViewModel(tripRepo, places)
        compose.waitForIdle()
        shot("TR_reg_place", content = { RegisterTripScreen(vm, {}, {}) })
    }

    @Test
    fun 배차등록_운임() {
        val vm = RegisterTripViewModel(tripRepo, places)
        compose.waitForIdle()
        vm.choose(StopChoice.Known(1, "평택 포승", "경기 평택시"))
        vm.choose(StopChoice.Known(2, "대구 성서", "대구 달서구"))
        vm.whenDone()
        vm.addFare(45)
        shot("TR_reg_money", content = { RegisterTripScreen(vm, {}, {}) })
    }

    @Test
    fun 배차등록_시각() {
        val vm = RegisterTripViewModel(tripRepo, places)
        compose.waitForIdle()
        vm.choose(StopChoice.Known(1, "평택 포승", "경기 평택시"))
        vm.choose(StopChoice.Known(2, "대구 성서", "대구 달서구"))
        shot("TR_reg_when", content = { RegisterTripScreen(vm, {}, {}) })
    }

    /** 시스템 글자 크기를 키워도 깨지지 않는지 본다 */
    @Test
    fun 큰글씨_오더() {
        val vm = orders()
        shot("LARGE_orders", content = {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.4f)) { OrdersScreen(vm, askLocation = false) }
        })
    }

    @Test
    fun 큰글씨_배차등록() {
        val vm = RegisterTripViewModel(tripRepo, places)
        compose.waitForIdle()
        shot("LARGE_register", content = {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.4f)) { RegisterTripScreen(vm, {}, {}) }
        })
    }

    private val day = LocalDate.now(ZoneId.of("Asia/Seoul"))

    private fun entry(id: Long, paid: Boolean, overdue: Boolean, broker: String) = LedgerEntry(
        id, true, day.minusDays(id), "평택 포승 → 대구 성서", 450_000, PaymentType.RECEIPT, broker, 280, 40, day.plusDays(20), paid, overdue,
        listOf(ExtraIncome(id * 10, StopType.PICKUP, "평택 포승", 30_000, false)),
    )

    private fun ledger() = LedgerViewModel(object : LedgerRepository {
        override suspend fun today() = ApiResult.Success(DailySummary(day, 1, 320_000))
        override suspend fun export(from: java.time.LocalDate, to: java.time.LocalDate): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1, 2))
        override suspend fun receivables() = ApiResult.Success(Receivables(870_000, 450_000, 30_000, listOf(entry(1, false, true, "△△로지스"), entry(2, false, false, "○○물류"))))
        override suspend fun entries(from: LocalDate, to: LocalDate) = ApiResult.Success(
            listOf(entry(1, false, true, "△△로지스"), entry(2, false, false, "○○물류"), entry(3, true, false, "한빛물류")) +
                (1L..5L).map { entry(40 + it, true, false, "한빛물류").copy(workDate = day.minusMonths(it), fare = 2_000_000L + it * 500_000) },
        )
        override suspend fun setPaid(entryId: Long, paid: Boolean) = ApiResult.Success(entry(1, paid, false, "△△로지스"))
        override suspend fun setExtraIncomePaid(extraIncomeId: Long, paid: Boolean) = ApiResult.Success(Unit)
        override suspend fun update(entry: LedgerEntry, fare: Long, paymentType: PaymentType, distanceKm: Int?) = ApiResult.Success(entry)
        override suspend fun addManual(entry: ManualEntry) = ApiResult.Success(entry(9, false, false, "x"))
        override suspend fun deleteEntry(entryId: Long) = ApiResult.Success(Unit)
        override suspend fun taxInvoice(entryId: Long): ApiResult<com.ieo.mobile.domain.ledger.TaxInvoice> = ApiResult.Failure(com.ieo.mobile.core.network.ApiError.Unexpected)
        override suspend fun issueTaxInvoice(entryId: Long): ApiResult<com.ieo.mobile.domain.ledger.TaxInvoice> = ApiResult.Failure(com.ieo.mobile.core.network.ApiError.Unexpected)
    }, object : ExpenseRepository {
        override suspend fun expenses(from: LocalDate, to: LocalDate) = ApiResult.Success(listOf(
            Expense(1, day, ExpenseCategory.FUEL, 180_000, null), Expense(2, day, ExpenseCategory.TOLL, 32_000, "대구 톨"), Expense(3, day.minusDays(1), ExpenseCategory.MEAL, 9_000, null),
        ))
        override suspend fun add(category: ExpenseCategory, amount: Long, memo: String?) = ApiResult.Success(Expense(9, day, category, amount, memo))
        override suspend fun delete(expenseId: Long) = ApiResult.Success(Unit)
    })

    @Test
    fun M10_수익() {
        val vm = ledger()
        shot("M10_earnings", content = { LedgerScreen(vm) })
    }

    @Test
    fun 장부상세() {
        val vm = ledger()
        compose.waitForIdle()
        vm.openEntry(vm.state.value.entries.first())
        shot("LG_detail", content = { LedgerOverlay(vm) })
    }

    @Test
    fun 못받은돈() {
        val vm = ledger()
        compose.waitForIdle()
        vm.openReceivables()
        shot("LG_owed", content = { LedgerOverlay(vm) })
    }

    @Test
    fun 경비추가() {
        val vm = ledger()
        compose.waitForIdle()
        vm.openExpense()
        vm.addAmount(100_000)
        shot("LG_expense", content = { LedgerOverlay(vm) })
    }

    @Test
    fun 단골() {
        val vm = LinksViewModel(object : DriverLinkRepository {
            override suspend fun invitations() = ApiResult.Success(listOf(DriverLink(1, "한빛물류", DriverLinkStatus.PENDING, Instant.now().plusSeconds(86_400 * 3), null)))
            override suspend fun links() = ApiResult.Success(listOf(DriverLink(2, "○○물류", DriverLinkStatus.LINKED, null, Instant.now().minusSeconds(86_400))))
            override suspend fun accept(linkId: Long) = ApiResult.Failure(ApiError.Network)
            override suspend fun decline(linkId: Long) = ApiResult.Success(Unit)
            override suspend fun unlink(linkId: Long) = ApiResult.Success(Unit)
        })
        shot("LK_list", content = { LinksScreen(vm, onBack = {}) })
    }

    @Test
    fun 알림() {
        val now = java.time.Instant.now()
        val items = listOf(
            AppNotification(3, NotificationType.DISPATCH_CONFIRMED, "한빛물류에서 배차를 확정했어요. 이천 물류센터에서 부산 신항까지, 내 배차에 넣었어요", 1, now.minusSeconds(120)),
            AppNotification(2, NotificationType.DISPATCH_DECLINED, "평택항에서 대전 복합터미널까지 가는 배차는 마감됐어요", 2, now.minusSeconds(7200)),
            AppNotification(1, NotificationType.DISPATCH_EXPIRED, "안산 공단에서 광주 하남까지 가는 배차 요청이 10분 안에 확정되지 않았어요", 3, now.minusSeconds(200_000)),
        )
        val store = object : NotificationSeenStore { override var lastSeenId: Long? = 2 }
        val vm = NotificationsViewModel(object : NotificationRepository {
            override suspend fun recent() = ApiResult.Success(items)
            override fun stream(after: Long?) = kotlinx.coroutines.flow.emptyFlow<NotificationStreamEvent>()
        }, store)
        vm.load()
        compose.waitForIdle()
        vm.open()
        shot("NT_inbox", content = {
            Box(Modifier.fillMaxSize()) {
                NotificationsScreen(vm, onOpen = {}, onBack = {})
                Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) { NotificationBanner(items[0], onDismiss = {}, onOpen = {}) }
                Box(Modifier.align(androidx.compose.ui.Alignment.TopEnd)) { NotificationBell(12, onClick = {}) }
            }
        })
    }

    private fun siteVm() = SiteCardViewModel(object : SiteRepository {
        val now = java.time.Instant.now()
        override suspend fun card(siteId: Long) = ApiResult.Success(
            SiteCard(
                1, "평택 포승 물류센터", "경기 평택시 포승읍 평택항로 156", 36.9, 126.8,
                SiteAttributes(3.5, true, false, now.minusSeconds(7200)),
                HeightCheck(HeightLevel.BLOCKED, 3.8, 3.5),
                SiteStats(42, 25, 12),
                listOf(
                    SiteNote(2, "정문 말고 후문으로 들어가요. 오전엔 줄이 길어요", 3, false, now.minusSeconds(86_400)),
                    SiteNote(1, "경비실에 차 번호 말하면 열어 줘요", 0, true, now.minusSeconds(600)),
                ),
            ),
        )
        override suspend fun addAttribute(siteId: Long, key: SiteAttributeKey, value: String) = ApiResult.Success(SiteAttributes())
        override suspend fun addNote(siteId: Long, text: String) = ApiResult.Success(SiteNote(9, text, 0, true, now))
    })

    @Test
    fun 현장정보() {
        val vm = siteVm()
        vm.open(1)
        compose.waitForIdle()
        shot("ST_view", content = { SiteCardScreen(vm) })
    }

    @Test
    fun 현장정보고치기() {
        val vm = siteVm()
        vm.open(1)
        compose.waitForIdle()
        vm.startEdit()
        shot("ST_edit", content = { SiteCardScreen(vm) })
    }

    @Test
    fun 현장메모() {
        val vm = siteVm()
        vm.open(1)
        compose.waitForIdle()
        vm.startNote()
        vm.setNote("후문은 저녁 6시에 닫아요")
        shot("ST_note", content = { SiteCardScreen(vm) })
    }

    /** 종이 인수증처럼 보이는 사진 (흰 종이에 줄 몇 개와 빨간 도장) */
    private fun paperJpeg(): ByteArray {
        val bitmap = android.graphics.Bitmap.createBitmap(300, 400, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(250, 248, 240))
        val ink = android.graphics.Paint().apply { color = android.graphics.Color.rgb(90, 90, 90) }
        for (i in 0 until 9) canvas.drawRect(30f, 50f + i * 34, if (i % 3 == 2) 180f else 270f, 58f + i * 34, ink)
        canvas.drawCircle(225f, 340f, 34f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(210, 50, 40); style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f })
        return java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    private fun proofVm(status: DeliveryProofStatus, photos: Int = 0, noWaits: Boolean = false) = ProofViewModel(object : ProofRepository {
        val now = java.time.Instant.now()
        val paper = paperJpeg()
        override suspend fun waits(tripId: Long) = ApiResult.Success(
            if (noWaits) emptyList() else listOf(
                WaitRecord(StopType.PICKUP, "평택 포승", now.minusSeconds(14_400), now.minusSeconds(6_600), now.minusSeconds(4_000), 130, 10_000, true, false),
                WaitRecord(StopType.DROPOFF, "대구 성서", now.minusSeconds(1_500), null, null, null, null, true, false),
            ),
        )
        val proof = DeliveryProof(
            required = true,
            status = status,
            sentAt = now.takeIf { status == DeliveryProofStatus.SENT },
            photos = (1..photos).map { ReceiptPhoto(it.toLong()) },
            maxPhotos = 5,
            canUpload = status != DeliveryProofStatus.SENT,
        )
        override suspend fun waitProofLink(tripId: Long) = ApiResult.Success(ProofLink("http://x/w/a", now))
        override suspend fun editWait(tripId: Long, stop: StopType, times: WaitTimes) =
            ApiResult.Success(WaitRecord(stop, "평택 포승", times.arrivedAt, times.workStartedAt, times.departedAt, 0, 0, true, true))
        override suspend fun deliveryProof(tripId: Long) = ApiResult.Success(proof)
        override suspend fun uploadReceiptPhoto(tripId: Long, jpeg: ByteArray) = ApiResult.Success(proof)
        override suspend fun deleteReceiptPhoto(tripId: Long, photoId: Long) = ApiResult.Success(proof)
        override suspend fun sendReceipt(tripId: Long) = ApiResult.Success(proof)
        override suspend fun receiptPhoto(tripId: Long, photoId: Long) = ApiResult.Success(paper)
    })

    @Test
    fun 증빙() {
        val vm = proofVm(DeliveryProofStatus.NONE)
        vm.open(1)
        compose.waitForIdle()
        shot("PF_pending", content = { ProofScreen(vm, onClose = {}) })
    }

    @Test
    fun 증빙인수증() {
        val vm = proofVm(DeliveryProofStatus.PENDING, photos = 5, noWaits = true)
        vm.open(1)
        compose.waitForIdle()
        shot("PF_receipt", content = { ProofScreen(vm, onClose = {}) })
    }

    @Test
    fun 증빙보냄() {
        val vm = proofVm(DeliveryProofStatus.SENT, photos = 2, noWaits = true)
        vm.open(1)
        compose.waitForIdle()
        shot("PF_sent", content = { ProofScreen(vm, onClose = {}) })
    }

    @Test
    fun 추천설정() {
        val vm = RecommendationViewModel(object : RecommendationRepository {
            override suspend fun preference() = ApiResult.Success(RecommendationPreference(true, 30, 200_000, setOf(CargoCategory.MOVING)))
            override suspend fun save(preference: RecommendationPreference) = ApiResult.Success(preference)
        })
        compose.waitForIdle()
        vm.stepRadius(5)
        shot("RC_settings", content = { RecommendationScreen(vm, onBack = {}) })
    }

    @Test
    fun 홈() {
        shot("M07_home", content = { HomeScreen("김도윤", VehiclesState(vehicles = listOf(sample)), trip(1, TripStatus.IN_TRANSIT), 4_120_000, 870_000, 1, {}, {}, {}, {}, {}, {}) })
    }
    private fun documents(vararg statuses: com.ieo.mobile.domain.document.DocumentStatus?): com.ieo.mobile.domain.document.DriverDocuments {
        val types = listOf("BUSINESS_REGISTRATION" to "사업자등록증", "VEHICLE_REGISTRATION" to "자동차등록증", "DRIVER_LICENSE" to "운전면허증", "FREIGHT_QUALIFICATION" to "화물운송종사 자격증")
        val required = types.mapIndexed { i, (type, label) ->
            val status = statuses.getOrNull(i)
            val reject = if (status == com.ieo.mobile.domain.document.DocumentStatus.REJECTED) "글씨가 잘 안 보여요. 밝은 곳에서 다시 찍어 주세요" else null
            com.ieo.mobile.domain.document.DocumentItem(type, label, true, listOf("이름이 가입한 이름과 같은지"), status?.let { com.ieo.mobile.domain.document.DocumentSubmission(i + 1L, it, null, reject) })
        }
        val optional = listOf(
            com.ieo.mobile.domain.document.DocumentItem("CONSIGNMENT_CONTRACT", "위·수탁(지입) 계약서", false, emptyList(), null),
            com.ieo.mobile.domain.document.DocumentItem("CARGO_INSURANCE", "적재물배상책임보험증서", false, emptyList(), null),
        )
        val approved = statuses.count { it == com.ieo.mobile.domain.document.DocumentStatus.APPROVED }
        return com.ieo.mobile.domain.document.DriverDocuments(approved == 4, approved, 4, required + optional)
    }

    private fun documentsVm(docs: com.ieo.mobile.domain.document.DriverDocuments) =
        com.ieo.mobile.ui.document.DocumentsViewModel(object : com.ieo.mobile.domain.document.DocumentRepository {
            override suspend fun documents() = ApiResult.Success(docs)
            override suspend fun submit(type: String, jpeg: ByteArray, business: com.ieo.mobile.domain.document.BusinessInfo?) = ApiResult.Success(Unit)
        })

    @Test
    fun D01_서류_처음() = shot("D01_documents_empty", content = { com.ieo.mobile.ui.document.DocumentsScreen(documentsVm(documents())) })

    @Test
    fun D02_서류_진행중() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.entries
        shot("D02_documents_mixed", content = { com.ieo.mobile.ui.document.DocumentsScreen(documentsVm(documents(s[1], s[0], s[2], null))) })
    }

    @Test
    fun D03_홈_서류카드() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.entries
        shot("D03_home_documents", content = { HomeScreen("김도윤", VehiclesState(vehicles = listOf(sample)), null, 4_120_000, 0, 0, {}, {}, {}, {}, {}, {}, documents = documents(s[1], s[1], null, null)) })
    }

    @Test
    fun D04_홈_인증차주() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.APPROVED
        shot("D04_home_verified", content = { HomeScreen("김도윤", VehiclesState(vehicles = listOf(sample)), null, 4_120_000, 0, 0, {}, {}, {}, {}, {}, {}, documents = documents(s, s, s, s)) })
    }

    @Test
    fun D05_서류_인증완료() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.APPROVED
        shot("D05_documents_verified", content = { com.ieo.mobile.ui.document.DocumentsScreen(documentsVm(documents(s, s, s, s))) })
    }

    @Test
    fun D06_서류올리기_페이지() {
        val vm = documentsVm(documents())
        compose.waitForIdle()
        vm.edit(vm.state.value.documents!!.required.first())
        shot("D06_upload_business", content = { com.ieo.mobile.ui.document.DocumentsScreen(vm) })
    }

    @Test
    fun M11_내정보_인증차주() {
        val accounts = object : AccountRepository {
            override suspend fun me() = ApiResult.Success(Account("김도윤", "01012345678"))
            override suspend fun rename(name: String) = ApiResult.Success(Account(name, "01012345678"))
            override suspend fun changePhone(phoneToken: String) = ApiResult.Success(Account("김도윤", "01099998888"))
            override suspend fun withdraw() = ApiResult.Success(Unit)
            override fun signOut() = Unit
        }
        val vm = MyViewModel(accounts)
        val approved = com.ieo.mobile.domain.document.DocumentStatus.APPROVED
        shot("M11_my_verified", content = { MyScreen(vm, VehiclesState(vehicles = listOf(sample)), 2, 0, "켬 · 30km", onVehicles = {}, onLinks = {}, onRecommendation = {}, documents = documents(approved, approved, approved, approved), record = com.ieo.mobile.domain.document.DriverRecord(true, 37, 1)) })
    }

    private fun samplePlan(code: com.ieo.mobile.domain.plan.PlanCode, renews: Boolean = true, trial: Boolean = false, gift: Boolean = false): com.ieo.mobile.domain.plan.MyPlan {
        val options = listOf(
            com.ieo.mobile.domain.plan.PlanOption(com.ieo.mobile.domain.plan.PlanCode.BASIC, "베이직", 0, listOf("오더·배차·운행 안내", "대기 증빙 30일 3건", "세금계산서 월 10장")),
            com.ieo.mobile.domain.plan.PlanOption(com.ieo.mobile.domain.plan.PlanCode.PRO, "프로", 30_000, listOf("대기 증빙 무제한", "장부 전체 기간·내보내기", "세금계산서 월 40장·자동 발행")),
        )
        val paid = code != com.ieo.mobile.domain.plan.PlanCode.BASIC
        return com.ieo.mobile.domain.plan.MyPlan(
            code, options.first { it.code == code }.label, options.first { it.code == code }.price, trial, !paid, renews && !trial && !gift, gift,
            if (paid) java.time.LocalDate.of(2026, 12, 12) else null, if (paid) (if (gift) 3 else 23) else null,
            com.ieo.mobile.domain.plan.PlanUsage(2, if (paid) null else 3, 4, if (paid) 40 else 10), options, true,
            if (gift) listOf(
                com.ieo.mobile.domain.plan.PlanCoupon(2, 3, "주선사 늦은 취소", java.time.LocalDate.of(2026, 11, 13), java.time.LocalDate.of(2027, 2, 11), used = false, usable = true),
                com.ieo.mobile.domain.plan.PlanCoupon(1, 3, "주선사 늦은 취소", java.time.LocalDate.of(2026, 11, 2), java.time.LocalDate.of(2027, 1, 31), used = true, usable = false),
            ) else emptyList(),
        )
    }

    private fun planVm(plan: com.ieo.mobile.domain.plan.MyPlan) = com.ieo.mobile.ui.plan.PlanViewModel(object : com.ieo.mobile.domain.plan.PlanRepository {
        override suspend fun mine() = ApiResult.Success(plan)
        override suspend fun choose(code: com.ieo.mobile.domain.plan.PlanCode) = ApiResult.Success(plan)
        override suspend fun startTrial() = ApiResult.Success(plan)
        override suspend fun resume() = ApiResult.Success(plan)
        override suspend fun useCoupon(couponId: Long) = ApiResult.Success(plan)
    })

    @Test
    fun C01_플랜_베이직() {
        val plan = samplePlan(com.ieo.mobile.domain.plan.PlanCode.BASIC)
        shot("C01_plan_basic", content = { com.ieo.mobile.ui.plan.PlanScreen(planVm(plan), com.ieo.mobile.ui.plan.PlanState(plan), onBack = {}) })
    }

    @Test
    fun C02_플랜_프로() {
        val plan = samplePlan(com.ieo.mobile.domain.plan.PlanCode.PRO)
        shot("C02_plan_pro", content = { com.ieo.mobile.ui.plan.PlanScreen(planVm(plan), com.ieo.mobile.ui.plan.PlanState(plan), onBack = {}) })
    }

    @Test
    fun C03_플랜_보상() {
        val plan = samplePlan(com.ieo.mobile.domain.plan.PlanCode.PRO, gift = true)
        shot("C03_plan_bonus", content = { com.ieo.mobile.ui.plan.PlanScreen(planVm(plan), com.ieo.mobile.ui.plan.PlanState(plan), onBack = {}) })
    }

    @Test
    fun C05_쿠폰함() {
        val plan = samplePlan(com.ieo.mobile.domain.plan.PlanCode.PRO, gift = true)
        shot("C05_coupon_box", content = {
            com.ieo.mobile.ui.plan.CouponBoxScreen(planVm(plan), com.ieo.mobile.ui.plan.PlanState(plan), onBack = {}, today = java.time.LocalDate.of(2026, 11, 13))
        })
    }

    @Test
    fun C04_요금_결제_안내() = shot("C04_plan_guide", content = { com.ieo.mobile.ui.plan.PlanGuideScreen(onBack = {}) })

    @Test
    fun D07_서류_반려_상세() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.entries
        val vm = documentsVm(documents(s[1], s[0], s[2], null))
        compose.waitForIdle()
        vm.edit(vm.state.value.documents!!.required[2])
        shot("D07_document_rejected", content = { com.ieo.mobile.ui.document.DocumentsScreen(vm) })
    }

    @Test
    fun D08_서류_확인중_상세() {
        val s = com.ieo.mobile.domain.document.DocumentStatus.entries
        val vm = documentsVm(documents(s[1], s[0], s[2], null))
        compose.waitForIdle()
        vm.edit(vm.state.value.documents!!.required[1])
        shot("D08_document_pending", content = { com.ieo.mobile.ui.document.DocumentsScreen(vm) })
    }

    @Test
    fun M12_내정보수정_번호인증() {
        val accounts = object : AccountRepository {
            override suspend fun me() = ApiResult.Success(Account("김도윤", "01012345678"))
            override suspend fun rename(name: String) = ApiResult.Success(Account(name, "01012345678"))
            override suspend fun changePhone(phoneToken: String) = ApiResult.Success(Account("김도윤", "01099998888"))
            override suspend fun withdraw() = ApiResult.Success(Unit)
            override fun signOut() = Unit
        }
        val vm = com.ieo.mobile.ui.my.ProfileEditViewModel(accounts, FakeAuth())
        vm.start(Account("김도윤", "01012345678"))
        vm.onPhone("01099998888")
        vm.sendCode()
        compose.waitForIdle()
        shot("M12_profile_edit", content = { com.ieo.mobile.ui.my.ProfileEditScreen(vm, onBack = {}, onSaved = {}) })
    }
}
