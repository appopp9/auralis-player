package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.StatsRepository
import com.auralis.player.domain.model.ListeningStats
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.TrendingSong
import com.auralis.player.domain.model.WrappedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val stats: StateFlow<ListeningStats> = statsRepository.stats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ListeningStats(0, 0L, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )

    val wrapped: StateFlow<WrappedData?> = statsRepository.wrapped

    val trending: StateFlow<List<TrendingSong>> = statsRepository.trending

    val duplicates: StateFlow<List<List<Song>>> = musicRepository.duplicates

    fun clearHistory() {
        viewModelScope.launch { statsRepository.clearHistory() }
    }
}
