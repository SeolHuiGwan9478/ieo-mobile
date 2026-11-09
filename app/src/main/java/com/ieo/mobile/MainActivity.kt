package com.ieo.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ieo.mobile.ui.proof.ProofScreen
import com.ieo.mobile.ui.proof.ProofViewModel
import com.ieo.mobile.ui.site.SiteCardScreen
import com.ieo.mobile.ui.site.SiteCardViewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ieo.mobile.domain.notification.NotificationType
import com.ieo.mobile.ui.notification.NotificationBanner
import com.ieo.mobile.ui.notification.NotificationsScreen
import com.ieo.mobile.ui.notification.NotificationsViewModel
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ieo.mobile.di.AppContainer
import com.ieo.mobile.domain.carlogin.CarLoginCode
import com.ieo.mobile.ui.carlogin.CarLoginScreen
import com.ieo.mobile.ui.carlogin.CarLoginViewModel
import com.ieo.mobile.ui.carlogin.QrScannerScreen
import com.ieo.mobile.ui.home.HomeScreen
import com.ieo.mobile.ui.login.LoginScreen
import com.ieo.mobile.ui.login.LoginViewModel
import com.ieo.mobile.ui.link.LinksScreen
import com.ieo.mobile.ui.link.LinksViewModel
import com.ieo.mobile.ui.my.AccountSection
import com.ieo.mobile.ui.my.AccountSettingsScreen
import com.ieo.mobile.ui.my.AccountSettingsViewModel
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
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.trip.RegisterTripScreen
import com.ieo.mobile.ui.trip.RegisterTripViewModel
import com.ieo.mobile.ui.trip.TripDetailScreen
import com.ieo.mobile.ui.trip.TripsScreen
import com.ieo.mobile.ui.trip.TripsViewModel
import com.ieo.mobile.ui.theme.White
import com.ieo.mobile.ui.vehicle.VehiclesScreen
import com.ieo.mobile.ui.vehicle.VehiclesViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** 기본 카메라로 QR을 찍어 들어온 코드. 로그인 전이면 로그인 뒤에 승인 화면을 띄운다 */
    private val deepLinkCode = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readDeepLink(intent)
        val container = (application as IeoApplication).container
        setContent {
            IeoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    IeoApp(container, deepLinkCode)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDeepLink(intent)
    }

    private fun readDeepLink(intent: Intent?) {
        intent?.dataString?.let(CarLoginCode::fromQr)?.let { deepLinkCode.value = it }
    }
}

/** 로그인 상태가 바뀌면 화면 상태를 통째로 비워 앞 계정의 내용과 오류가 남지 않게 한다. 화면 회전에는 살아남는다 */
class LoginScope : ViewModel(), ViewModelStoreOwner {
    override var viewModelStore = ViewModelStore()
        private set
    private var loggedIn: Boolean? = null

    fun sync(now: Boolean) {
        if (loggedIn != null && loggedIn != now) {
            viewModelStore.clear()
            viewModelStore = ViewModelStore()
        }
        loggedIn = now
    }

    override fun onCleared() = viewModelStore.clear()
}

@Composable
private fun IeoApp(container: AppContainer, deepLinkCode: MutableStateFlow<String?>) {
    val loggedIn by container.session.isLoggedIn.collectAsStateWithLifecycle()
    val scope: LoginScope = androidx.lifecycle.viewmodel.compose.viewModel()
    scope.sync(loggedIn)
    CompositionLocalProvider(LocalViewModelStoreOwner provides scope) {
        IeoContent(container, deepLinkCode, loggedIn)
    }
}

@Composable
private fun IeoContent(container: AppContainer, deepLinkCode: MutableStateFlow<String?>, loggedIn: Boolean) {
    val linked by deepLinkCode.collectAsStateWithLifecycle()
    LaunchedEffect(loggedIn) { if (!loggedIn) container.notificationSeenStore.lastSeenId = null }
    var scanning by remember { mutableStateOf(false) }

    if (!loggedIn) {
        LoginScreen(viewModel(key = "login") { LoginViewModel(container.authRepository) })
        return
    }
    val code = linked
    when {
        code != null -> {
            BackHandler { deepLinkCode.value = null }
            CarLoginScreen(
                code = code,
                viewModel = viewModel(key = "car-login-$code") { CarLoginViewModel(container.carLoginRepository) },
                onClose = { deepLinkCode.value = null },
            )
        }
        scanning -> {
            BackHandler { scanning = false }
            QrScannerScreen(
                onCode = { scanning = false; deepLinkCode.value = it },
                onClose = { scanning = false },
            )
        }
        else -> Tabs(container, onScan = { scanning = true })
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Outlined.Home),
    ORDERS("오더", Icons.Outlined.Search),
    TRIPS("배차", Icons.Outlined.LocalShipping),
    EARNINGS("수익", Icons.AutoMirrored.Outlined.ShowChart),
    MY("내 정보", Icons.Outlined.Person),
}

/** 시안의 아래 탭 */
@Composable
private fun Tabs(container: AppContainer, onScan: () -> Unit) {
    val notifications: NotificationsViewModel = viewModel(key = "notifications") {
        NotificationsViewModel(container.notificationRepository, container.notificationSeenStore)
    }
    val notificationState by notifications.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(notifications) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { notifications.listen() } }
    Box(Modifier.fillMaxSize()) {
        TabsContent(container, onScan, notifications)
        notificationState.banner?.let { NotificationBanner(it, notifications::dismissBanner) }
    }
}

@Composable
private fun TabsContent(container: AppContainer, onScan: () -> Unit, notifications: NotificationsViewModel) {
    val notificationState by notifications.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var managingVehicles by rememberSaveable { mutableStateOf(false) }
    var registering by rememberSaveable { mutableStateOf(false) }
    var managingLinks by rememberSaveable { mutableStateOf(false) }
    var accountSection by rememberSaveable { mutableStateOf<AccountSection?>(null) }
    var editingRecommendation by rememberSaveable { mutableStateOf(false) }
    val recommendation: RecommendationViewModel = viewModel(key = "recommendation") { RecommendationViewModel(container.recommendationRepository) }
    val recommendationState by recommendation.state.collectAsStateWithLifecycle()
    val links: LinksViewModel = viewModel(key = "links") { LinksViewModel(container.driverLinkRepository) }
    val linkState by links.state.collectAsStateWithLifecycle()
    val my: MyViewModel = viewModel(key = "my") { MyViewModel(container.accountRepository) }
    val accountSettings: AccountSettingsViewModel = viewModel(key = "accountSettings") { AccountSettingsViewModel(container.accountSettingsRepository) }
    val accountSettingsState by accountSettings.state.collectAsStateWithLifecycle()
    val vehicles: VehiclesViewModel = viewModel(key = "vehicles") { VehiclesViewModel(container.vehicleRepository) }
    val orders: OrdersViewModel = viewModel(key = "orders") { OrdersViewModel(container.orderRepository, container.locationSource) }
    val trips: TripsViewModel = viewModel(key = "trips") { TripsViewModel(container.tripRepository) }
    val ledger: LedgerViewModel = viewModel(key = "ledger") { LedgerViewModel(container.ledgerRepository, container.expenseRepository) }
    val ledgerState by ledger.state.collectAsStateWithLifecycle()
    val orderState by orders.state.collectAsStateWithLifecycle()
    val tripState by trips.state.collectAsStateWithLifecycle()
    val myState by my.state.collectAsStateWithLifecycle()
    val vehicleState by vehicles.state.collectAsStateWithLifecycle()
    LaunchedEffect(notifications) {
        notifications.received.collect {
            trips.refresh()
            orders.refresh()
        }
    }

    // 차량 앱이 운행을 진행시키므로 앱으로 돌아올 때마다 배차를 새로 받는다
    val lifecycleForTrips = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(trips) {
        var returning = false
        lifecycleForTrips.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (returning) trips.refresh()
            returning = true
        }
    }
    val site: SiteCardViewModel = viewModel(key = "site") { SiteCardViewModel(container.siteRepository) }
    val siteState by site.state.collectAsStateWithLifecycle()
    val proof: ProofViewModel = viewModel(key = "proof") { ProofViewModel(container.proofRepository) }
    val proofState by proof.state.collectAsStateWithLifecycle()
    if (proofState.tripId != null) {
        ProofScreen(proof, onClose = trips::refresh)
        return
    }
    if (siteState.siteId != null) {
        SiteCardScreen(site)
        return
    }
    if (notificationState.isOpen) {
        NotificationsScreen(
            notifications,
            onOpen = {
                notifications.close()
                tab = if (it.type == NotificationType.DISPATCH_CONFIRMED) Tab.TRIPS else Tab.ORDERS
            },
            onBack = notifications::close,
        )
        return
    }
    if (managingVehicles) {
        VehiclesScreen(vehicles, onBack = { managingVehicles = false })
        return
    }
    if (editingRecommendation) {
        RecommendationScreen(recommendation, onBack = { editingRecommendation = false })
        return
    }
    accountSection?.let { section ->
        AccountSettingsScreen(section, accountSettings, onBack = { accountSection = null })
        return
    }
    if (managingLinks) {
        LinksScreen(links, onBack = { managingLinks = false })
        return
    }
    if (registering) {
        // 등록할 때마다 처음부터 시작하도록 ViewModel을 새로 만든다
        val store = remember { ViewModelStore() }
        DisposableEffect(Unit) { onDispose { store.clear() } }
        val register: RegisterTripViewModel = viewModel(
            viewModelStoreOwner = remember { object : ViewModelStoreOwner { override val viewModelStore = store } },
            factory = factoryOf { RegisterTripViewModel(container.tripRepository, container.placeSearch) },
        )
        RegisterTripScreen(register, onClose = { registering = false }, onDone = {
            registering = false
            trips.refresh()
            tab = Tab.TRIPS
        })
        return
    }
    if (ledgerState.hasOverlay) {
        LedgerOverlay(ledger)
        return
    }
    orderState.selected?.let { order ->
        OrderDetailScreen(order, orderState, orders, onSite = site::open)
        return
    }
    tripState.selected?.let { trip ->
        TripDetailScreen(trip, tripState, trips, onSite = site::open, onProof = { id -> proof.open(id, trip.canUploadPaperProof) })
        return
    }
    // 다른 탭에서 뒤로 가기를 누르면 앱을 끄지 않고 홈으로 돌아간다
    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    name = myState.account?.name,
                    vehicles = vehicleState,
                    nextTrip = tripState.next,
                    monthNet = ledgerState.ledger.takeIf { !ledgerState.isLoading }?.netProfit,
                    owed = ledgerState.owedTotal,
                    invitationCount = linkState.invitations.size,
                    onInvitations = { managingLinks = true },
                    onEarnings = { tab = Tab.EARNINGS },
                    onScan = onScan,
                    onVehicles = { managingVehicles = true },
                    onOpenTrip = { trips.open(it.id) },
                    onFindOrders = { tab = Tab.ORDERS },
                    unreadNotifications = notificationState.unreadCount,
                    onNotifications = notifications::open,
                )
                Tab.ORDERS -> OrdersScreen(
                    orders,
                    hasVehicle = vehicleState.isLoading || vehicleState.vehicles.isNotEmpty(),
                    onAddVehicle = { managingVehicles = true },
                )
                Tab.TRIPS -> TripsScreen(trips, onRegister = { registering = true })
                Tab.EARNINGS -> LedgerScreen(ledger)
                Tab.MY -> MyScreen(my, vehicleState, linkState.links.size, linkState.invitations.size, recommendationState.saved?.summary, onVehicles = { managingVehicles = true }, onLinks = { managingLinks = true }, carSessionCount = accountSettingsState.carSessions.size, onAccountSection = { accountSection = it }, onRecommendation = { editingRecommendation = true })
            }
        }
        Row(Modifier.fillMaxWidth().background(White).padding(vertical = 6.dp)) {
            Tab.entries.forEach { item ->
                val color = if (item == tab) Orange700 else TabOff
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).clickable { tab = item },
                ) {
                    Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                    Text(item.label, fontSize = 14.sp, fontWeight = if (item == tab) FontWeight.Bold else FontWeight.Medium, color = color)
                }
            }
        }
    }
}

private val TabOff = com.ieo.mobile.ui.theme.TextSub

private inline fun <reified VM : androidx.lifecycle.ViewModel> factoryOf(noinline create: () -> VM) =
    viewModelFactory { initializer { create() } }

@Composable
private inline fun <reified VM : androidx.lifecycle.ViewModel> viewModel(key: String, noinline create: () -> VM): VM =
    viewModel(key = key, factory = factoryOf(create))
