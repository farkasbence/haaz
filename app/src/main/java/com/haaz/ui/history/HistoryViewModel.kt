package com.haaz.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haaz.data.HistoryRepository
import com.haaz.data.HistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val history: StateFlow<HistoryState> = historyRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryState()
    )

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing

    fun clearHistory() {
        if (_isClearing.value) return
        viewModelScope.launch {
            _isClearing.value = true
            historyRepository.clear()
            _isClearing.value = false
        }
    }
}
