package com.ieo.mobile.domain.account

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/** 내 계정으로 로그인된 차량 한 대 */
data class CarSession(val id: String, val name: String?, val lastUsedAt: Instant)

/** 동의 항목 하나의 지금 상태. [type]은 서버 ConsentType 이름이다 */
data class ConsentItem(val type: String, val description: String, val required: Boolean, val agreed: Boolean)

data class BusinessInfo(val name: String, val number: String, val representative: String)

/** 내 정보의 설정: 로그인된 차량, 동의 관리, 사업자 정보 (ACC-003·005·006) */
interface AccountSettingsRepository {

    suspend fun carSessions(): ApiResult<List<CarSession>>

    suspend fun revokeCarSession(id: String): ApiResult<Unit>

    suspend fun consents(): ApiResult<List<ConsentItem>>

    suspend fun changeConsent(type: String, agreed: Boolean): ApiResult<ConsentItem>

    /** 등록하지 않았으면 null */
    suspend fun business(): ApiResult<BusinessInfo?>

    suspend fun saveBusiness(info: BusinessInfo): ApiResult<BusinessInfo?>

    suspend fun clearBusiness(): ApiResult<Unit>
}
