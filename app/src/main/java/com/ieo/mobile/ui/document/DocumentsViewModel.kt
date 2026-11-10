package com.ieo.mobile.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.document.BusinessInfo
import com.ieo.mobile.domain.document.DocumentItem
import com.ieo.mobile.domain.document.DocumentRepository
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.domain.document.DriverRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentsState(
    val documents: DriverDocuments? = null,
    /** 주선사에게 보이는 내 이력. 못 받으면 null */
    val record: DriverRecord? = null,
    val isOpen: Boolean = false,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** 올리기 페이지를 연 서류 종류 */
    val editing: String? = null,
    /** 올리는 중인 서류 종류 */
    val uploading: String? = null,
    val message: String? = null,
    val error: String? = null,
) {
    /** 아직 모르면 막지 않는다. 서버가 한 번 더 막는다 */
    val verified: Boolean get() = documents?.verified ?: true
}

/** 내 서류 (07 인계서 3장): 필수 4종을 찍어 올리고, 심사 결과와 반려 사유를 본다. 다 승인되면 배차를 요청할 수 있다 */
class DocumentsViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _state = MutableStateFlow(DocumentsState())
    val state: StateFlow<DocumentsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun open() {
        _state.update { it.copy(isOpen = true, message = null, error = null) }
        refresh()
    }

    fun close() = _state.update { it.copy(isOpen = false, editing = null, message = null, error = null) }

    fun edit(item: DocumentItem) = _state.update { it.copy(editing = item.type, message = null, error = null) }

    fun stopEditing() = _state.update { it.copy(editing = null, error = null) }

    fun refresh() {
        viewModelScope.launch {
            (repository.record() as? ApiResult.Success)?.let { result -> _state.update { it.copy(record = result.data) } }
        }
        viewModelScope.launch {
            when (val result = repository.documents()) {
                is ApiResult.Success -> _state.update { it.copy(documents = result.data, isLoading = false, loadFailed = false) }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, loadFailed = it.documents == null, error = if (it.isOpen) result.error.message else null)
                }
            }
        }
    }

    fun submit(item: DocumentItem, jpeg: ByteArray, business: BusinessInfo?) {
        if (_state.value.uploading != null) return
        _state.update { it.copy(uploading = item.type, message = null, error = null) }
        viewModelScope.launch {
            when (val result = repository.submit(item.type, jpeg, business)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(uploading = null, editing = null, message = "${objectOf(item.label)} 올렸어요. 심사 결과는 여기서 볼 수 있어요") }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(uploading = null, error = result.error.message) }
            }
        }
    }

    fun photoUnreadable() = _state.update { it.copy(error = "사진을 읽지 못했어요. 다시 찍어 주세요") }
}

/** "사업자등록증을", "보험증서를": 받침에 따라 을/를을 붙인다 */
internal fun objectOf(word: String): String {
    val last = word.lastOrNull() ?: return word
    val hasBatchim = last in '가'..'힣' && (last - '가') % 28 != 0
    return word + if (hasBatchim) "을" else "를"
}
