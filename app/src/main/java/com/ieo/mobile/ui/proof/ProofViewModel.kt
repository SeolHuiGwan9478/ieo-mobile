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
    val waits: List<WaitRecord> = emptyList(),
    val delivery: DeliveryProof? = null,
    /** 받아 둔 인수증 사진 (사진 id → 파일) */
    val photoFiles: Map<Long, ByteArray> = emptyMap(),
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
    data object Upload : ProofAction
    data class DeletePhoto(val photoId: Long) : ProofAction
    data object SendReceipt : ProofAction
}

/** 대기 증빙과 인수증. 대기 증빙 링크는 폰의 공유 창으로, 종이 인수증 사진은 서버로 주선사에게 보낸다 (WAIT-020, 022, POD-009, 011) */
class ProofViewModel(private val repository: ProofRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProofState())
    val state: StateFlow<ProofState> = _state.asStateFlow()

    fun open(tripId: Long) {
        _state.value = ProofState(tripId = tripId, isLoading = true)
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
            (delivery as? ApiResult.Success)?.let { loadPhotos(tripId, it.data) }
        }
    }

    /** 아직 안 받은 사진만 받는다. 못 받은 사진은 자리만 보인다 */
    private suspend fun loadPhotos(tripId: Long, delivery: DeliveryProof) {
        delivery.photos.filter { it.id !in _state.value.photoFiles }.forEach { photo ->
            val file = (repository.receiptPhoto(tripId, photo.id) as? ApiResult.Success)?.data ?: return@forEach
            _state.update { if (it.tripId == tripId) it.copy(photoFiles = it.photoFiles + (photo.id to file)) else it }
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
                state.copy(waits = state.waits.map { if (it.stopType == record.stopType) result.data else it }, message = "시각을 수정했어요")
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun shareConsumed() = _state.update { it.copy(share = null) }

    fun photoUnreadable() = fail("사진을 읽지 못했어요. 다시 찍어 주세요")

    /** 종이 인수증 한 장 올리기. 올린 파일은 바로 목록에 보여 준다 */
    fun uploadPaper(jpeg: ByteArray) = run(ProofAction.Upload) { tripId ->
        if (_state.value.delivery?.canUpload != true) return@run
        when (val result = repository.uploadReceiptPhoto(tripId, jpeg)) {
            is ApiResult.Success -> _state.update { state ->
                val added = result.data.photos.map { it.id }.filter { it !in state.photoFiles }
                state.copy(delivery = result.data, photoFiles = state.photoFiles + added.associateWith { jpeg })
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun deletePhoto(photoId: Long) = run(ProofAction.DeletePhoto(photoId)) { tripId ->
        when (val result = repository.deleteReceiptPhoto(tripId, photoId)) {
            is ApiResult.Success -> _state.update { it.copy(delivery = result.data, photoFiles = it.photoFiles - photoId) }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    /** [주선사에게 보내기] (POD-011) */
    fun sendReceipt() = run(ProofAction.SendReceipt) { tripId ->
        when (val result = repository.sendReceipt(tripId)) {
            is ApiResult.Success -> _state.update { it.copy(delivery = result.data, message = "인수증을 주선사에게 보냈어요") }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }
}
