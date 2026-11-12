package com.ieo.mobile.data.plan

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.domain.plan.PlanCode
import com.ieo.mobile.domain.plan.PlanRepository

class DefaultPlanRepository(
    private val api: PlanApi,
    private val caller: ApiCaller,
) : PlanRepository {

    override suspend fun mine(): ApiResult<MyPlan> = caller.call { api.mine() }.mapResponse { it.toDomain() }

    override suspend fun choose(code: PlanCode): ApiResult<MyPlan> =
        caller.call { api.choose(ChoosePlanRequest(code.name)) }.mapResponse { it.toDomain() }

    override suspend fun startTrial(): ApiResult<MyPlan> = caller.call { api.startTrial() }.mapResponse { it.toDomain() }

    override suspend fun resume(): ApiResult<MyPlan> = caller.call { api.resume() }.mapResponse { it.toDomain() }
}
