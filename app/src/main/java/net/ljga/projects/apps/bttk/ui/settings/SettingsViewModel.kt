package net.ljga.projects.apps.bttk.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.database.AppDatabase
import net.ljga.projects.apps.bttk.data.database.entity.AppSettingsEntity
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isDebugEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val state: StateFlow<SettingsState> = database.appSettingsDao().getSettings()
        .map { settings ->
            SettingsState(
                isLoading = _isLoading.value,
                message = _message.value,
                isDebugEnabled = settings?.isDebugEnabled ?: false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsState()
        )

    fun toggleDebug(enabled: Boolean) {
        viewModelScope.launch {
            database.appSettingsDao().updateSettings(AppSettingsEntity(isDebugEnabled = enabled))
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.close()
                val dbFile = context.getDatabasePath("app_database")
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _message.value = "Database exported successfully"
            } catch (e: Exception) {
                _message.value = "Export failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.close()
                val dbFile = context.getDatabasePath("app_database")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    dbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                _message.value = "Database imported successfully. Please restart the app."
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
