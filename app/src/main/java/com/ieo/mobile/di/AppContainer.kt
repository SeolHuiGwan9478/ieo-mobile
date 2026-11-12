package com.ieo.mobile.di

import com.ieo.mobile.data.plan.DefaultPlanRepository
import com.ieo.mobile.data.plan.PlanApi
import com.ieo.mobile.domain.plan.PlanRepository
import com.ieo.mobile.data.document.DefaultDocumentRepository
import com.ieo.mobile.data.document.DocumentApi
import com.ieo.mobile.domain.document.DocumentRepository
import com.ieo.mobile.data.proof.DefaultProofRepository
import com.ieo.mobile.data.proof.ProofApi
import com.ieo.mobile.domain.proof.ProofRepository
import android.content.Context
import com.ieo.mobile.BuildConfig
import com.ieo.mobile.core.network.NetworkClients
import com.ieo.mobile.core.session.DeviceInfo
import com.ieo.mobile.core.session.EncryptedTokenStore
import com.ieo.mobile.core.session.KeystoreTokenCipher
import com.ieo.mobile.core.session.SessionManager
import com.ieo.mobile.data.account.AccountApi
import com.ieo.mobile.data.account.AccountSettingsApi
import com.ieo.mobile.data.account.DefaultAccountRepository
import com.ieo.mobile.data.account.DefaultAccountSettingsRepository
import com.ieo.mobile.data.auth.AuthApi
import com.ieo.mobile.data.auth.DefaultAuthRepository
import com.ieo.mobile.data.auth.ServerTokenRefresher
import com.ieo.mobile.data.carlogin.CarLoginApi
import com.ieo.mobile.data.carlogin.DefaultCarLoginRepository
import com.ieo.mobile.data.expense.DefaultExpenseRepository
import com.ieo.mobile.data.expense.ExpenseApi
import com.ieo.mobile.data.ledger.DefaultLedgerRepository
import com.ieo.mobile.data.ledger.LedgerApi
import com.ieo.mobile.data.link.DefaultDriverLinkRepository
import com.ieo.mobile.data.link.DriverLinkApi
import com.ieo.mobile.data.location.DeviceLocationSource
import com.ieo.mobile.data.location.GeocoderPlaceSearch
import com.ieo.mobile.data.notification.DefaultNotificationRepository
import com.ieo.mobile.data.notification.NotificationApi
import com.ieo.mobile.data.notification.PrefsNotificationSeenStore
import com.ieo.mobile.data.site.DefaultSiteRepository
import com.ieo.mobile.data.site.SiteApi
import com.ieo.mobile.data.recommendation.DefaultRecommendationRepository
import com.ieo.mobile.data.recommendation.RecommendationApi
import com.ieo.mobile.data.trip.DefaultTripRepository
import com.ieo.mobile.data.trip.TripApi
import com.ieo.mobile.data.order.DefaultOrderRepository
import com.ieo.mobile.data.order.OrderApi
import com.ieo.mobile.data.vehicle.DefaultVehicleRepository
import com.ieo.mobile.data.vehicle.VehicleApi
import com.ieo.mobile.domain.account.AccountRepository
import com.ieo.mobile.domain.account.AccountSettingsRepository
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.carlogin.CarLoginRepository
import com.ieo.mobile.domain.expense.ExpenseRepository
import com.ieo.mobile.domain.ledger.LedgerRepository
import com.ieo.mobile.domain.link.DriverLinkRepository
import com.ieo.mobile.domain.order.LocationSource
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.notification.NotificationRepository
import com.ieo.mobile.domain.notification.NotificationSeenStore
import com.ieo.mobile.domain.site.SiteRepository
import com.ieo.mobile.domain.recommendation.RecommendationRepository
import com.ieo.mobile.domain.trip.PlaceSearch
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.domain.vehicle.VehicleRepository

/** 앱 전체에서 하나만 있는 객체를 손으로 연결한다 */
class AppContainer(context: Context) {

    val session = SessionManager(EncryptedTokenStore(context, KeystoreTokenCipher()))
    private val device = DeviceInfo(context, BuildConfig.VERSION_NAME)
    private val network = NetworkClients(
        baseUrl = BuildConfig.API_BASE_URL,
        session = session,
        enableLogging = BuildConfig.DEBUG,
        refresherFactory = { ServerTokenRefresher(it.create(AuthApi::class.java)) },
    )

    val authRepository: AuthRepository =
        DefaultAuthRepository(network.public.create(AuthApi::class.java), network.caller, session, device)
    val accountRepository: AccountRepository =
        DefaultAccountRepository(network.authenticated.create(AccountApi::class.java), network.caller, session, device)
    val accountSettingsRepository: AccountSettingsRepository =
        DefaultAccountSettingsRepository(network.authenticated.create(AccountSettingsApi::class.java), network.caller)
    val vehicleRepository: VehicleRepository =
        DefaultVehicleRepository(network.authenticated.create(VehicleApi::class.java), network.caller)
    val orderRepository: OrderRepository =
        DefaultOrderRepository(network.authenticated.create(OrderApi::class.java), network.caller)
    val locationSource: LocationSource = DeviceLocationSource(context)
    val tripRepository: TripRepository =
        DefaultTripRepository(network.authenticated.create(TripApi::class.java), network.caller)
    val placeSearch: PlaceSearch = GeocoderPlaceSearch(context, locationSource)
    val ledgerRepository: LedgerRepository =
        DefaultLedgerRepository(network.authenticated.create(LedgerApi::class.java), network.caller)
    val expenseRepository: ExpenseRepository =
        DefaultExpenseRepository(network.authenticated.create(ExpenseApi::class.java), network.caller)
    val planRepository: PlanRepository =
        DefaultPlanRepository(network.authenticated.create(PlanApi::class.java), network.caller)
    val driverLinkRepository: DriverLinkRepository =
        DefaultDriverLinkRepository(network.authenticated.create(DriverLinkApi::class.java), network.caller)
    val recommendationRepository: RecommendationRepository =
        DefaultRecommendationRepository(network.authenticated.create(RecommendationApi::class.java), network.caller)
    val notificationSeenStore: NotificationSeenStore = PrefsNotificationSeenStore(context)
    val notificationRepository: NotificationRepository = DefaultNotificationRepository(
        network.authenticated.create(NotificationApi::class.java),
        network.caller,
        network.authenticatedClient,
        BuildConfig.API_BASE_URL,
        network.json,
    )
    val siteRepository: SiteRepository =
        DefaultSiteRepository(network.authenticated.create(SiteApi::class.java), network.caller)
    val proofRepository: ProofRepository =
        DefaultProofRepository(network.authenticated.create(ProofApi::class.java), network.caller, BuildConfig.API_BASE_URL)
    val documentRepository: DocumentRepository =
        DefaultDocumentRepository(network.authenticated.create(DocumentApi::class.java), network.caller)
    val carLoginRepository: CarLoginRepository =
        DefaultCarLoginRepository(network.authenticated.create(CarLoginApi::class.java), network.caller)
}
