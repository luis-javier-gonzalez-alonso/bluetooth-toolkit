package net.ljga.projects.apps.bttk.ui.dataframe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.database.entities.DataFrame
import net.ljga.projects.apps.bttk.ui.dataframe.DataFrameUiState.Error
import net.ljga.projects.apps.bttk.ui.dataframe.DataFrameUiState.Loading
import net.ljga.projects.apps.bttk.ui.dataframe.DataFrameUiState.Success
import javax.inject.Inject

@HiltViewModel
class DataFrameViewModel @Inject constructor(
    private val dataFrameRepository: DataFrameRepository
) : ViewModel() {

    val uiState: StateFlow<DataFrameUiState> = dataFrameRepository
        .dataFrames.map<List<DataFrame>, DataFrameUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun addDataFrame(name: String, data: ByteArray) {
        viewModelScope.launch {
            dataFrameRepository.add(name, data)
        }
    }
    
    fun deleteDataFrame(uid: Int) {
        viewModelScope.launch {
            dataFrameRepository.remove(uid)
        }
    }
}

sealed interface DataFrameUiState {
    object Loading : DataFrameUiState
    data class Error(val throwable: Throwable) : DataFrameUiState
    data class Success(val data: List<DataFrame>) : DataFrameUiState
}
