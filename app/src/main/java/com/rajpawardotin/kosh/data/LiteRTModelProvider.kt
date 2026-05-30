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
            if (backend == "NPU (Qualcomm)") {
                val libsToLoad = listOf(
                    "LiteRt",
                    "QnnSystem",
                    "QnnHtp",
                    "LiteRtDispatch_Qualcomm"
                )
                for (lib in libsToLoad) {
                    try {
                        android.util.Log.i("KOSH_NPU", "Manually loading lib$lib.so...")
                        System.loadLibrary(lib)
                        android.util.Log.i("KOSH_NPU", "Successfully loaded lib$lib.so")
                    } catch (e: UnsatisfiedLinkError) {
                        android.util.Log.e("KOSH_NPU", "Failed to load lib$lib.so: ${e.message}", e)
                    } catch (e: Exception) {
                        android.util.Log.e("KOSH_NPU", "Error loading lib$lib.so: ${e.message}", e)
                    }
                }
            }

            val litertBackend = when (backend) {
                "GPU" -> Backend.GPU()
                "NPU (Qualcomm)" -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
                else -> {
                    // Restrict CPU threads to 4 to target performance cores and avoid scheduling on
                    // slow efficiency cores. This reduces cache thrashing, synchronization barriers, and heat.
                    Backend.CPU(numOfThreads = 4)
                }
            }
            val config = EngineConfig(
                modelPath = modelPath,
                backend = litertBackend,
                cacheDir = context.cacheDir.absolutePath
            )
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            conversation = null // Do not pre-allocate conversation to avoid FAILED_PRECONDITION
            currentModelPath = modelPath
            currentBackend = backend
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sendMessage(prompt: String): Flow<String> = callbackFlow {
        val eng = engine ?: run {
            this@callbackFlow.close(IllegalStateException("Engine not initialized"))
            return@callbackFlow
        }

        // Close any pre-existing active conversation to avoid FAILED_PRECONDITION: A session already exists.
        val oldConv = conversation
        conversation = null
        if (oldConv != null) {
            try {
                oldConv.cancelProcess()
                oldConv.close()
            } catch (e: Exception) {
                // Ignore
            }
        }

        val freshConv = eng.createConversation()
        conversation = freshConv

        freshConv.sendMessageAsync(prompt, object : MessageCallback {
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
                freshConv.cancelProcess()
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
