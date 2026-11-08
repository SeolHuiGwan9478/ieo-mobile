package com.ieo.mobile.domain.auth

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.vehicle.NewVehicle
import java.time.Instant

/** 가입할 때 받는 동의 (서버 ConsentType). [required]는 차주 기준 */
enum class Consent(val label: String, val required: Boolean, val hint: String? = null) {
    TERMS("이용약관", true),
    PRIVACY("개인정보 수집·이용", true),
    LOCATION("위치 정보 수집", true, "운행 안내·대기 기록에 써요"),
    BROKER_SHARE("주선사에 위치·운행 정보 공유", false, "도착 예정 시각을 자동으로 알려요"),
    VOICE("음성 데이터 처리", false, "차량에서 말로 답할 때 써요"),
    MARKETING("마케팅 정보 받기", false, "문자·푸시 · 혜택 소식"),
}

/** 이 번호로 이미 가입한 계정. 날짜는 모르면 null */
data class ExistingAccount(
    val name: String,
    val isDriver: Boolean,
    val joinedAt: Instant? = null,
    val lastLoginAt: Instant? = null,
)

/** 인증번호 확인 결과. [phoneToken]을 로그인이나 가입에 한 번 쓴다 */
data class PhoneVerification(val phoneToken: String, val existingAccount: ExistingAccount?)

interface AuthRepository {

    /** 인증번호를 보낸다. 로컬 서버에서만 시연용 인증번호가 돌아온다 */
    suspend fun requestCode(phone: String): ApiResult<String?>

    suspend fun verify(phone: String, code: String): ApiResult<PhoneVerification>

    /** 성공하면 로그인 상태가 된다 */
    suspend fun login(phoneToken: String): ApiResult<Unit>

    /** 차주로 가입한다. 가입 완료 화면을 보여 주도록 [finishSignUp]을 부를 때 로그인 상태가 된다 */
    suspend fun signUp(phoneToken: String, name: String, consents: Set<Consent>, vehicle: NewVehicle?): ApiResult<Unit>

    fun finishSignUp()
}
