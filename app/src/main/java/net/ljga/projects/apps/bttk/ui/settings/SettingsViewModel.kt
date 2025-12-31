package net.ljga.projects.apps.bttk.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.database.AppDatabase
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                database.close()
                val dbFile = context.getDatabasePath("app_database")
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _state.value = _state.value.copy(isLoading = false, message = "Database exported successfully")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, message = "Export failed: ${e.message}")
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                database.close()
                val dbFile = context.getDatabasePath("app_database")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    dbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _state.value = _state.value.copy(isLoading = false, message = "Database imported successfully. Please restart the app.")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, message = "Import failed: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
