/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ljga.projects.apps.bttk.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.ljga.projects.apps.bttk.data.database.repository.BluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.database.repository.CharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.database.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.data.database.repository.DefaultBluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.database.repository.DefaultCharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.database.repository.DefaultDataFrameRepository
import net.ljga.projects.apps.bttk.data.database.repository.DefaultSavedDeviceRepository
import net.ljga.projects.apps.bttk.data.database.repository.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.database.entity.DataFrame
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsDataFrameRepository(
        dataFrameRepository: DefaultDataFrameRepository
    ): DataFrameRepository

    @Singleton
    @Binds
    fun bindsSavedDeviceRepository(
        savedDeviceRepository: DefaultSavedDeviceRepository
    ): SavedDeviceRepository

    @Singleton
    @Binds
    fun bindsCharacteristicParserRepository(
        characteristicParserRepository: DefaultCharacteristicParserRepository
    ): CharacteristicParserRepository

    @Singleton
    @Binds
    fun bindsBluetoothScriptRepository(
        bluetoothScriptRepository: DefaultBluetoothScriptRepository
    ): BluetoothScriptRepository
}

class FakeDataFrameRepository @Inject constructor() : DataFrameRepository {
    override val dataFrames: Flow<List<DataFrame>> = flowOf(fakeDataFrames)

    override suspend fun add(name: String, data: ByteArray) {
        throw NotImplementedError()
    }

    override suspend fun remove(uid: Int) {
        throw NotImplementedError()
    }
}

val fakeDataFrames = listOf(
    DataFrame(name = "One", data = byteArrayOf(1, 2, 3)),
    DataFrame(name = "Two", data = byteArrayOf(4, 5, 6)),
    DataFrame(name = "Three", data = byteArrayOf(7, 8, 9))
)
