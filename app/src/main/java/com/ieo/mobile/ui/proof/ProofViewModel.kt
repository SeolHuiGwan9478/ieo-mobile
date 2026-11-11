package com.ieo.mobile.ui.proof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.ProofRepository
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.proof.WaitTimes
import com.ieo.mobile.domain.trip.StopType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProofState(
    val tripId: Long? = null,
    val paperAllowed: Boolean = true,
    val waits: List<WaitRecord> = emptyList(),
    val delivery: DeliveryProof? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    /** 어떤 버튼이 일하는 중인지 */
    val busy: ProofAction? = null,
    val error: String? = null,
    val message: String? = null,
    /** 공유 창에 넘길 글. 한 번 쓰면 비운다 */
    val share: String? = null,
)

sealed interface ProofAction {
    data object WaitLink : ProofAction
    data class EditWait(val stop: StopType) : ProofAction
    data object DeliveryLink : ProofAction
    data object Upload : ProofAction
    data object Check : ProofAction
}

/** 대기 증빙과 인수증. 링크는 폰의 공유 창으로 보낸다 (WAIT-020, 022, POD-001, 009) */
class ProofViewModel(private val repository: ProofRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProofState())
    val state: StateFlow<ProofState> = _state.asStateFlow()

    fun open(tripId: Long, paperAllowed: Boolean = true) {
        _state.value = ProofState(tripId = tripId, paperAllowed = paperAllowed, isLoading = true)
        load(tripId)
    }

    fun close() {
        _state.value = ProofState()
    }

    fun retry() {
        val id = _state.value.tripId ?: return
        _state.update { it.copy(isLoading = true, loadFailed = false, error = null) }
        load(id)
    }

    private fun load(tripId: Long) {
        viewModelScope.launch {
            val waits = repository.waits(tripId)
            val delivery = repository.deliveryProof(tripId)
            val failure = (waits as? ApiResult.Failure) ?: (delivery as? ApiResult.Failure)
            _state.update {
                when {
                    it.tripId != tripId -> it
                    failure != null -> it.copy(isLoading = false, loadFailed = true, error = failure.error.message)
                    else -> it.copy(
                        isLoading = false,
                        waits = (waits as ApiResult.Success).data.sortedBy { record -> record.stopType.ordinal },
                        delivery = (delivery as ApiResult.Success).data,
                    )
                }
            }
        }
    }

    private fun run(action: ProofAction, block: suspend (Long) -> Unit) {
        val state = _state.value
        val tripId = state.tripId ?: return
        if (state.busy != null) return
        _state.update { it.copy(busy = action, error = null, message = null) }
        viewModelScope.launch {
            block(tripId)
            _state.update { it.copy(busy = null) }
        }
    }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    /** 운행 하나에 링크 하나: 상차지·하차지 기록을 함께 보여 준다 (WAIT-020) */
    fun shareWaitProof() = run(ProofAction.WaitLink) { tripId ->
        when (val result = repository.waitProofLink(tripId)) {
            is ApiResult.Success -> _state.update { it.copy(share = "대기 증빙이에요. 상차지·하차지 도착·작업 시각과 대기 시간을 확인해 주세요.\n${result.data.url}") }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    /** 대기 시각 고치기 (WAIT-006). 고친 기록으로 바꿔 끼운다 */
    fun editWait(record: WaitRecord, times: WaitTimes) = run(ProofAction.EditWait(record.stopType)) { tripId ->
        when (val result = repository.editWait(tripId, record.stopType, times)) {
            is ApiResult.Success -> _state.update { state ->
                state.copy(waits = state.waits.map { if (it.stopType == record.stopType) result.data else it }, message = "시각을 고쳤어요")
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun shareDeliveryLink() = run(ProofAction.DeliveryLink) { tripId ->
        when (val result = repository.issueDeliveryProof(tripId)) {
            is ApiResult.Success -> _state.update {
                it.copy(delivery = result.data, share = "화물 인수 확인을 부탁드려요. 아래 링크에서 내용을 확인하고 서명해 주세요.\n${result.data.url}")
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun shareConsumed() = _state.update { it.copy(share = null) }

    fun checkSigned() = run(ProofAction.Check) { tripId ->
        when (val result = repository.deliveryProof(tripId)) {
            is ApiResult.Success -> _state.update {
                it.copy(delivery = result.data, message = if (result.data?.isSigned == true) null else "아직 서명 전이에요")
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun photoUnreadable() = fail("사진을 읽지 못했어요. 다시 찍어 주세요")

    /** 서버는 사진을 받으면 인수가 끝난 것으로 치고, 인수증이 먼저 있어야 받는다 */
    fun uploadPaper(jpeg: ByteArray) = run(ProofAction.Upload) { tripId ->
        if (!_state.value.paperAllowed) return@run
        if (_state.value.delivery == null) {
            when (val issued = repository.issueDeliveryProof(tripId)) {
                is ApiResult.Success -> _state.update { it.copy(delivery = issued.data) }
                is ApiResult.Failure -> {
                    fail(issued.error.message)
                    return@run
                }
            }
        }
        when (val result = repository.uploadPaperPhoto(tripId, jpeg)) {
            is ApiResult.Success -> _state.update { it.copy(delivery = result.data, message = "종이 인수증을 올렸어요") }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }
}
