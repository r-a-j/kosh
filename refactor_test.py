import sys
import re

file_path = "app/src/test/java/com/rajpawardotin/kosh/ui/chat/ChatViewModelTest.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace FakeChatRepository declarations
content = content.replace("private lateinit var fakeRepository: FakeChatRepository", 
                          "private lateinit var fakeSessionRepo: FakeSessionRepository\n    private lateinit var fakeMessageRepo: FakeMessageRepository\n    private lateinit var fakeDocumentRepo: FakeDocumentRepository\n    private lateinit var fakeTts: FakeTtsProvider")

content = content.replace("fakeRepository = FakeChatRepository()", 
                          "fakeSessionRepo = FakeSessionRepository()\n        fakeMessageRepo = FakeMessageRepository()\n        fakeDocumentRepo = FakeDocumentRepository()\n        fakeTts = FakeTtsProvider()")

content = content.replace("viewModel = ChatViewModel(fakeAI, fakeSearch, fakeRepository, fakeSettings, testDispatcher)",
                          "viewModel = ChatViewModel(fakeAI, fakeSearch, fakeSessionRepo, fakeMessageRepo, fakeDocumentRepo, fakeSettings, fakeTts, testDispatcher)")

# Replace fakeRepository usages
content = content.replace("fakeRepository.sessions", "fakeSessionRepo.sessions")
content = content.replace("fakeRepository.saveSession(", "fakeSessionRepo.saveSession(")
content = content.replace("fakeRepository.messages", "fakeMessageRepo.messages")
content = content.replace("fakeRepository.saveMessage(", "fakeMessageRepo.saveMessage(")
content = content.replace("fakeRepository.saveChecklistState(", "fakeMessageRepo.saveChecklistState(")
content = content.replace("fakeRepository.getMessagesForSession(", "fakeMessageRepo.getMessagesForSession(")

# Fix testTextChunkingSlidingWindow
content = content.replace("val method = ChatViewModel::class.java.getDeclaredMethod(\"chunkText\", String::class.java, Int::class.java, Int::class.java)", 
                          "val usecase = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase(fakeDocumentRepo)\n        val method = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase::class.java.getDeclaredMethod(\"chunkText\", String::class.java, Int::class.java, Int::class.java)")
content = content.replace("val chunks = method.invoke(viewModel, longText, 1000, 200) as List<String>", 
                          "val chunks = method.invoke(usecase, longText, 1000, 200) as List<String>")

# Add FakeTtsProvider at the end
fake_tts_code = """
    class FakeTtsProvider : com.rajpawardotin.kosh.data.TtsProvider {
        override val isSpeaking = kotlinx.coroutines.flow.MutableStateFlow(false)
        override fun speak(text: String, voiceName: String?) {}
        override fun stop() {}
        override fun shutdown() {}
    }
"""
content = content.replace("}\n}", "}\n" + fake_tts_code + "\n}")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
