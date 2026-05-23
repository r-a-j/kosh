package com.rajpawardotin.kosh.data

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsProviderImpl(context: Context) : TtsProvider, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    override val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    private var currentMessageId: String? = null
    private var utteranceCounter = 0

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Handled synchronously in speak()
                }

                override fun onDone(utteranceId: String?) {
                    // Check if this is the last chunk
                    if (utteranceId != null && utteranceId.endsWith("_LAST")) {
                        if (currentMessageId != null && utteranceId.startsWith(currentMessageId!!)) {
                            _currentlySpeakingMessageId.value = null
                            currentMessageId = null
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    _currentlySpeakingMessageId.value = null
                    currentMessageId = null
                }
            })
            isInitialized = true
        }
    }

    override fun speak(messageId: String, text: String) {
        if (!isInitialized || tts == null) return

        stop() // Stop any current speech
        currentMessageId = messageId
        _currentlySpeakingMessageId.value = messageId

        // Chunk text by natural sentence boundaries or max 3000 chars
        val chunks = splitTextForTts(text)
        
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val utteranceId = if (i == chunks.lastIndex) "${messageId}_LAST" else "${messageId}_$i"
            tts?.speak(chunk, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    override fun stop() {
        tts?.stop()
        _currentlySpeakingMessageId.value = null
        currentMessageId = null
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    private fun splitTextForTts(text: String, maxLength: Int = 3000): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n", "\r\n")
        
        for (para in paragraphs) {
            if (para.isBlank()) continue
            
            // If the paragraph is already smaller than maxLength, just add it
            if (para.length <= maxLength) {
                result.add(para)
            } else {
                // Split long paragraphs by sentences
                val sentences = para.split(Regex("(?<=[.!?])\\s+"))
                var currentChunk = ""
                for (sentence in sentences) {
                    if ((currentChunk.length + sentence.length) <= maxLength) {
                        currentChunk += if (currentChunk.isEmpty()) sentence else " $sentence"
                    } else {
                        if (currentChunk.isNotEmpty()) {
                            result.add(currentChunk)
                        }
                        currentChunk = sentence
                    }
                }
                if (currentChunk.isNotEmpty()) {
                    result.add(currentChunk)
                }
            }
        }
        return result
    }
}
