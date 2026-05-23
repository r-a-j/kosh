package com.rajpawardotin.kosh.data

import kotlinx.coroutines.flow.StateFlow

interface TtsProvider {
    val currentlySpeakingMessageId: StateFlow<String?>

    fun speak(messageId: String, text: String)
    fun stop()
    fun shutdown()
}
