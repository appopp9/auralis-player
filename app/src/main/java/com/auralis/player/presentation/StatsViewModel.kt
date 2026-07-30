package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.StatsRepository
import com.auralis.player.domain.model.ListeningStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    val stats: StateFlow<ListeningStats> = statsRepository.stats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ListeningStats(0, 0L, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )

    fun clearHistory() {
        viewModelScope.launch { statsRepository.clearHistory() }
    }
}
