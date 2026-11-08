package com.ieo.mobile.ui.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.site.SiteAttributeKey
import com.ieo.mobile.domain.site.SiteCard
import com.ieo.mobile.domain.site.SiteHeight
import com.ieo.mobile.domain.site.SiteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SiteMode { VIEW, EDIT_INFO, WRITE_NOTE }

data class SiteInfoDraft(val height: String = "", val forklift: Boolean? = null, val parking: Boolean? = null)

data class SiteCardState(
    val siteId: Long? = null,
    val card: SiteCard? = null,
    val mode: SiteMode = SiteMode.VIEW,
    val draft: SiteInfoDraft = SiteInfoDraft(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val loadError: String? = null,
    val error: String? = null,
    val saved: String? = null,
) {
    val canSaveNote: Boolean get() = note.isNotBlank() && note.length <= MAX_NOTE

    companion object {
        const val MAX_NOTE = 500
    }
}

/** 상하차지 정보 카드 (SITE-004, 007, 008). 서버는 항목마다 최신 값을 쓰므로 바뀐 항목만 보낸다 */
class SiteCardViewModel(private val repository: SiteRepository) : ViewModel() {

    private val _state = MutableStateFlow(SiteCardState())
    val state: StateFlow<SiteCardState> = _state.asStateFlow()

    fun open(siteId: Long) {
        _state.value = SiteCardState(siteId = siteId, isLoading = true)
        load(siteId)
    }

    fun close() {
        _state.value = SiteCardState()
    }

    fun retry() {
        _state.value.siteId?.let { id ->
            _state.update { it.copy(isLoading = true, loadError = null) }
            load(id)
        }
    }

    private fun load(siteId: Long) {
        viewModelScope.launch {
            when (val result = repository.card(siteId)) {
                is ApiResult.Success -> _state.update { if (it.siteId == siteId) it.copy(card = result.data, isLoading = false) else it }
                is ApiResult.Failure -> _state.update { if (it.siteId == siteId) it.copy(isLoading = false, loadError = result.error.message) else it }
            }
        }
    }

    fun startEdit() {
        val attributes = _state.value.card?.attributes ?: return
        _state.update {
            it.copy(
                mode = SiteMode.EDIT_INFO,
                draft = SiteInfoDraft(attributes.heightLimitM?.toString().orEmpty(), attributes.forklift, attributes.parking),
                error = null,
                saved = null,
            )
        }
    }

    fun startNote() = _state.update { it.copy(mode = SiteMode.WRITE_NOTE, note = "", error = null, saved = null) }

    fun backToView() = _state.update { it.copy(mode = SiteMode.VIEW, error = null) }

    fun setHeight(text: String) = _state.update { it.copy(draft = it.draft.copy(height = text.take(5)), error = null) }

    fun setForklift(value: Boolean) = _state.update { it.copy(draft = it.draft.copy(forklift = value), error = null) }

    fun setParking(value: Boolean) = _state.update { it.copy(draft = it.draft.copy(parking = value), error = null) }

    fun setNote(text: String) = _state.update { it.copy(note = text.take(SiteCardState.MAX_NOTE), error = null) }

    fun saveInfo() {
        val state = _state.value
        val card = state.card ?: return
        if (state.isBusy) return
        val changes = mutableListOf<Pair<SiteAttributeKey, String>>()
        if (state.draft.height.isNotBlank()) {
            val height = SiteHeight.normalize(state.draft.height)
            if (height == null) {
                _state.update { it.copy(error = "높이 제한은 ${SiteHeight.MIN_M}m에서 ${SiteHeight.MAX_M.toInt()}m 사이 숫자로 적어 주세요") }
                return
            }
            if (height.toDouble() != card.attributes.heightLimitM) changes += SiteAttributeKey.HEIGHT_LIMIT to height
        }
        state.draft.forklift?.takeIf { it != card.attributes.forklift }?.let { changes += SiteAttributeKey.FORKLIFT to it.toString() }
        state.draft.parking?.takeIf { it != card.attributes.parking }?.let { changes += SiteAttributeKey.PARKING to it.toString() }
        if (changes.isEmpty()) {
            backToView()
            return
        }
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            for ((key, value) in changes) {
                val result = repository.addAttribute(card.id, key, value)
                if (result is ApiResult.Failure) {
                    // 앞 항목은 이미 저장됐을 수 있어 카드를 새로 받는다
                    load(card.id)
                    _state.update { it.copy(isBusy = false, error = result.error.message) }
                    return@launch
                }
            }
            load(card.id)
            _state.update { it.copy(isBusy = false, mode = SiteMode.VIEW, saved = "고마워요. 다른 기사님에게 큰 도움이 돼요") }
        }
    }

    fun saveNote() {
        val state = _state.value
        val card = state.card ?: return
        if (state.isBusy || !state.canSaveNote) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.addNote(card.id, state.note.trim())) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        card = it.card?.copy(notes = listOf(result.data) + it.card.notes),
                        isBusy = false,
                        mode = SiteMode.VIEW,
                        note = "",
                        saved = "메모를 남겼어요",
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(isBusy = false, error = result.error.message) }
            }
        }
    }
}
