package net.ljga.projects.apps.bttk.ui.dataframe


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import net.ljga.projects.apps.bttk.data.database.repository.DataFrameRepository

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalCoroutinesApi::class) // TODO: Remove when stable
class DataFrameViewModelTest {
    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = DataFrameViewModel(FakeDataFrameRepository())
        assertEquals(viewModel.uiState.first(), DataFrameUiState.Loading)
    }

    @Test
    fun uiState_onItemSaved_isDisplayed() = runTest {
        val viewModel = DataFrameViewModel(FakeDataFrameRepository())
        assertEquals(viewModel.uiState.first(), DataFrameUiState.Loading)
    }
}

private class FakeDataFrameRepository : DataFrameRepository {

    private val data = mutableListOf<String>()

    override val dataFrames: Flow<List<String>>
        get() = flow { emit(data.toList()) }

    override suspend fun add(name: String) {
        data.add(0, name)
    }
}
