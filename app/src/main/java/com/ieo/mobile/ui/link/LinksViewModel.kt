package com.ieo.mobile.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.domain.link.DriverLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LinksState(
    val invitations: List<DriverLink> = emptyList(),
    val links: List<DriverLink> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/** 단골 주선사 (BRK-002): 받은 요청 수락·거절, 연결된 회사, 연결 해제. 단골 관리는 휴대폰에서만 한다 */
class LinksViewModel(private val repository: DriverLinkRepository) : ViewModel() {

    private val _state = MutableStateFlow(LinksState())
    val state: StateFlow<LinksState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            reload()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun accept(link: DriverLink) = change("${link.brokerName} 연결을 마쳤어요") { repository.accept(link.id).unit() }

    fun decline(link: DriverLink) = change("요청을 거절했어요") { repository.decline(link.id) }

    fun unlink(link: DriverLink) = change("연결을 해제했어요") { repository.unlink(link.id) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    private fun <T> ApiResult<T>.unit(): ApiResult<Unit> = when (this) {
        is ApiResult.Success -> ApiResult.Success(Unit)
        is ApiResult.Failure -> this
    }

    private fun change(notice: String, block: suspend () -> ApiResult<Unit>) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(notice = notice) }
                    reload()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(error = result.error.message) }
                    // 이미 만료된 요청일 수 있으니 목록을 맞춘다
                    reload(keepError = true)
                }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun reload(keepError: Boolean = false) {
        val invitations = repository.invitations()
        val links = repository.links()
        val failure = listOf(invitations, links).filterIsInstance<ApiResult.Failure>().firstOrNull()
        _state.update {
            it.copy(
                invitations = (invitations as? ApiResult.Success)?.data ?: it.invitations,
                links = (links as? ApiResult.Success)?.data ?: it.links,
                loadFailed = failure != null,
                error = if (keepError) it.error else failure?.error?.message,
            )
        }
    }
}
