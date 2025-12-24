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

package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.local.database.DataFrame
import net.ljga.projects.apps.bttk.data.local.database.DataFrameDao
import javax.inject.Inject

interface DataFrameRepository {
    val dataFrames: Flow<List<DataFrame>>

    suspend fun add(name: String, data: ByteArray)
    suspend fun remove(uid: Int)
}

class DefaultDataFrameRepository @Inject constructor(
    private val dataFrameDao: DataFrameDao
) : DataFrameRepository {

    override val dataFrames: Flow<List<DataFrame>> = dataFrameDao.getDataFrames()

    override suspend fun add(name: String, data: ByteArray) {
        dataFrameDao.insertDataFrame(DataFrame(name = name, data = data))
    }

    override suspend fun remove(uid: Int) {
        dataFrameDao.deleteDataFrame(uid)
    }
}
