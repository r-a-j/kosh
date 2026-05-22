package com.rajpawardotin.kosh.data

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.rajpawardotin.kosh.domain.provider.AIProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LiteRTModelProvider(private val context: Context) : AIProvider {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override var isInitialized: Boolean = false
        private set

    override suspend fun initialize(modelPath: String, backend: String): Result<Unit> {
        return try {
            val litertBackend = when (backend) {
                "GPU" -> Backend.GPU()
                "NPU (Qualcomm)" -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
                else -> Backend.CPU()
            }
            val config = EngineConfig(modelPath = modelPath, backend = litertBackend)
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            conversation = newEngine.createConversation()
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sendMessage(prompt: String): Flow<String> = callbackFlow {
        val currentConv = conversation ?: run {
            close()
            error("Engine not initialized")
        }

        currentConv.sendMessageAsync(prompt, object : MessageCallback {
            override fun onMessage(message: Message) {
                val textChunk = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .firstOrNull()?.text ?: ""
                trySend(textChunk)
            }
            override fun onDone() {
                close()
            }
            override fun onError(throwable: Throwable) {
                close(throwable)
            }
        })
        awaitClose { /* No-op: sendMessageAsync handles its own lifecycle */ }
    }

    override fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        isInitialized = false
    }
}

