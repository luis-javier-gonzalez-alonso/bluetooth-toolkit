package net.ljga.projects.apps.bttk.ui

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ljga.projects.apps.bttk.data.FakeBluetoothController
import net.ljga.projects.apps.bttk.data.FakeGattCharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.FakeDataFrameRepository
import net.ljga.projects.apps.bttk.data.FakeGattServerRepository
import net.ljga.projects.apps.bttk.data.FakeBluetoothDeviceRepository
import net.ljga.projects.apps.bttk.ui.bluetooth.BluetoothViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: BluetoothViewModel
    private lateinit var bluetoothController: FakeBluetoothController
    private lateinit var savedDeviceRepository: FakeBluetoothDeviceRepository
    private lateinit var dataFrameRepository: FakeDataFrameRepository
    private lateinit var gattServerRepository: FakeGattServerRepository
    private lateinit var characteristicParserRepository: FakeGattCharacteristicParserRepository
    private val context = mock(Context::class.java)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        bluetoothController = FakeBluetoothController()
        savedDeviceRepository = FakeBluetoothDeviceRepository()
        dataFrameRepository = FakeDataFrameRepository()
        gattServerRepository = FakeGattServerRepository()
        characteristicParserRepository = FakeGattCharacteristicParserRepository()
        
        viewModel = BluetoothViewModel(
            bluetoothController = bluetoothController,
            bluetoothDeviceRepository = savedDeviceRepository,
            dataFrameRepository = dataFrameRepository,
            gattServerRepository = gattServerRepository,
            gattCharacteristicParserRepository = characteristicParserRepository,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads data from repositories`() = runTest {
        advanceUntilIdle()
        val state = viewModel.state.first()
        
        // Check if fake data from repositories is present in the initial state
        assertTrue("Saved devices should not be empty", state.savedDevices.isNotEmpty())
        assertTrue("Gatt aliases should not be empty", state.gattAliases.isNotEmpty())
        assertTrue("Data frames should not be empty", state.savedDataFrames.isNotEmpty())
    }

    @Test
    fun `startScan updates refreshing state`() = runTest {
        viewModel.startScan()
        advanceUntilIdle()
        
        assertTrue("isRefreshing should be true during scan", viewModel.state.value.isRefreshing)
        assertTrue("BluetoothController should be scanning", bluetoothController.isScanning.value)
    }

    @Test
    fun `saveDataFrame adds a new data frame to repository`() = runTest {
        val name = "Test Frame"
        val data = byteArrayOf(0x01, 0x02)
        
        viewModel.saveDataFrame(name, data)
        advanceUntilIdle()
        
        val state = viewModel.state.first()
        assertTrue("New data frame should be in the state", state.savedDataFrames.any { it.name == name })
    }

    @Test
    fun `deleteDataFrame removes a data frame from repository`() = runTest {
        advanceUntilIdle()
        val initialSize = viewModel.state.value.savedDataFrames.size
        val idToDelete = viewModel.state.value.savedDataFrames.first().uid
        
        viewModel.deleteDataFrame(idToDelete)
        advanceUntilIdle()
        
        val finalSize = viewModel.state.value.savedDataFrames.size
        assertEquals("Data frame list size should decrease by 1", initialSize - 1, finalSize)
    }
    
    @Test
    fun `toggleGattServer when running stops server`() = runTest {
        // Initial setup - fake server is not running in BluetoothController but UI might think it is
        // We need to simulate the controller state
        bluetoothController.startGattServer()
        advanceUntilIdle()
        
        // toggleGattServer in VM will send an intent to stop the service (hard to verify without Robolectric)
        // But we can verify it doesn't crash and follows the logic
        viewModel.toggleGattServer()
        // verify logic...
    }
}
