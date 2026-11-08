package com.ieo.mobile.data.link

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.domain.link.DriverLinkRepository

class DefaultDriverLinkRepository(
    private val api: DriverLinkApi,
    private val caller: ApiCaller,
) : DriverLinkRepository {

    override suspend fun invitations(): ApiResult<List<DriverLink>> =
        caller.call { api.invitations() }.mapResponse { list -> list.map(DriverLinkResponse::toDomain) }

    override suspend fun links(): ApiResult<List<DriverLink>> =
        caller.call { api.links() }.mapResponse { list -> list.map(DriverLinkResponse::toDomain) }

    override suspend fun accept(linkId: Long): ApiResult<DriverLink> =
        caller.call { api.accept(linkId) }.mapResponse { it.toDomain() }

    override suspend fun decline(linkId: Long): ApiResult<Unit> = caller.callNoContent { api.decline(linkId) }

    override suspend fun unlink(linkId: Long): ApiResult<Unit> = caller.callNoContent { api.unlink(linkId) }
}
