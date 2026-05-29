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
    private var currentModelPath: String? = null
    private var currentBackend: String? = null

    @Volatile
    private var activeCloseThread: Thread? = null

    override var isInitialized: Boolean = false
        private set

    override suspend fun initialize(modelPath: String, backend: String): Result<Unit> {
        if (isInitialized && modelPath == currentModelPath && backend == currentBackend) {
            return Result.success(Unit)
        }
        activeCloseThread?.join()
        close()
        activeCloseThread?.join()
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
            currentModelPath = modelPath
            currentBackend = backend
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sendMessage(prompt: String): Flow<String> = callbackFlow {
        val currentConv = conversation ?: run {
            this@callbackFlow.close(IllegalStateException("Engine not initialized"))
            return@callbackFlow
        }

        currentConv.sendMessageAsync(prompt, object : MessageCallback {
            override fun onMessage(message: Message) {
                val textChunk = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .firstOrNull()?.text ?: ""
                trySend(textChunk)
            }
            override fun onDone() {
                this@callbackFlow.close()
            }
            override fun onError(throwable: Throwable) {
                this@callbackFlow.close(throwable)
            }
        })
        awaitClose {
            try {
                currentConv.cancelProcess()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun close() {
        val conv = conversation
        val eng = engine
        conversation = null
        engine = null
        isInitialized = false
        currentModelPath = null
        currentBackend = null

        if (conv != null) {
            try {
                conv.cancelProcess()
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (conv != null || eng != null) {
            // Run JNI close in a background thread to prevent native C++ deadlocks/hangs from blocking JVM threads.
            val t = Thread {
                try {
                    conv?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                try {
                    eng?.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            activeCloseThread = t
            t.start()
        }
    }
}
