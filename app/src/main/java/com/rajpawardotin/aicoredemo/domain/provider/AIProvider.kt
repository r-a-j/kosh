package com.rajpawardotin.aicoredemo.domain.provider

import kotlinx.coroutines.flow.Flow

interface AIProvider {
    val isInitialized: Boolean
    
    suspend fun initialize(modelPath: String, backend: String): Result<Unit>
    
    fun sendMessage(prompt: String): Flow<String>
    
    fun close()
}
