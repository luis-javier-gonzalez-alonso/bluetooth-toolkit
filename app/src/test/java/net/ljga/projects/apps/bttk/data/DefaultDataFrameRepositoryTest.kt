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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import net.ljga.projects.apps.bttk.data.database.entity.DataFrame
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.repository.DefaultDataFrameRepository

/**
 * Unit tests for [net.ljga.projects.apps.bttk.data.database.repository.DefaultDataFrameRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class) // TODO: Remove when stable
class DefaultDataFrameRepositoryTest {

    @Test
    fun dataFrames_newItemSaved_itemIsReturned() = runTest {
        val repository = DefaultDataFrameRepository(FakeDataFrameDao())

        repository.add("Repository")

        assertEquals(repository.dataFrames.first().size, 1)
    }

}

private class FakeDataFrameDao : DataFrameDao {

    private val data = mutableListOf<DataFrame>()

    override fun getDataFrames(): Flow<List<DataFrame>> = flow {
        emit(data)
    }

    override suspend fun insertDataFrame(item: DataFrame) {
        data.add(0, item)
    }
}
