package com.rajpawardotin.kosh.domain.agent

import kotlinx.coroutines.CompletableDeferred

interface PermissionRequester {
    suspend fun requestPermission(permission: String): Boolean
}

data class PermissionRequest(
    val permission: String,
    val deferred: CompletableDeferred<Boolean>
)
