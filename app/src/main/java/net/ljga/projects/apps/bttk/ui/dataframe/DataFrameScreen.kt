package net.ljga.projects.apps.bttk.ui.dataframe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ljga.projects.apps.bttk.data.database.entities.DataFrame
import net.ljga.projects.apps.bttk.ui.theme.MyApplicationTheme

@Composable
fun DataFrameScreen(modifier: Modifier = Modifier, viewModel: DataFrameViewModel = hiltViewModel()) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    if (items is DataFrameUiState.Success) {
        DataFrameScreen(
            items = (items as DataFrameUiState.Success).data,
            onSave = { name -> viewModel.addDataFrame(name, byteArrayOf()) },
            modifier = modifier
        )
    }
}

@Composable
internal fun DataFrameScreen(
    items: List<DataFrame>,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        var nameDataFrame by remember { mutableStateOf("Compose") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = nameDataFrame,
                onValueChange = { nameDataFrame = it }
            )

            Button(modifier = Modifier.width(96.dp), onClick = { onSave(nameDataFrame) }) {
                Text("Save")
            }
        }
        items.forEach {
            Text("Saved item: ${it.name}")
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MyApplicationTheme {
        DataFrameScreen(listOf(DataFrame("Compose", byteArrayOf())), onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    MyApplicationTheme {
        DataFrameScreen(listOf(DataFrame("Compose", byteArrayOf())), onSave = {})
    }
}
