package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain

interface GattServerRepository {
    val config: Flow<GattServerStateDomain>

    suspend fun saveConfig(state: GattServerStateDomain)
}
