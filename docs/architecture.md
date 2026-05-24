# Kosh System Architecture

Kosh is an offline-first, strictly private AI cognitive vault designed to operate entirely locally on an Android device. This document outlines the core architecture and feature modules of Kosh v1.1.0.

## Core Components

- **Repository (`ChatRepository`):** Central source of truth coordinating SQLite storage, Tink encryption, search indexing, and LiteRT memory. Handles seamless cross-database merging for backup imports.
- **Provider Interfaces:**
  - `AIProvider`: Agnostic wrapper around LiteRT (`LiteRTModelProvider`) and potentially ONNX/GGUF in the future. Isolated to prevent native crashes from affecting core app stability.
  - `SearchProvider`: Handles FTS4 and document chunking for local RAG.
  - `SettingsProvider`: Encrypted Tink storage for Keystore items and crash sentinels.
  - `TtsProvider`: Abstraction over Android TTS.
- **Resilience Architecture:** See [resilience.md](resilience.md) for details on our Native Crash Sentinel, JNI Fault Tolerance, SQLite Error Handlers, and Global Coroutine Handlers.

## 1. UI Layer (Jetpack Compose)
- **Declarative UX**: The entire user interface is built using Jetpack Compose, emphasizing a fluid, glassmorphic dark theme (`KoshTheme`).
- **Reactive State**: The UI consumes state from `ChatViewModel` via `StateFlow` and `mutableStateOf`. 
- **Dynamic Renderers**: Uses a custom Markdown parser to format AI output. It supports live "Thinking" indicators, typing effects, and dynamically updates when chunks are streamed from the LiteRT engine.

## 2. Neural Core (Local LLM)
- **LiteRT (TensorFlow Lite)**: The `LiteRTModelProvider` handles loading quantized `.litertlm` or `.bin` models into memory.
- **Hardware Acceleration**: Automatically delegates inference tasks to the Hexagon DSP (NPU) via `libcdsprpc.so`, or falls back to the GPU/CPU based on user preferences in the Settings panel.
- **Context Management**: Kosh dynamically truncates conversation history using a sliding window to fit within the model's maximum context window (e.g., 2048 or 4096 tokens).

## 3. Cryptography & Vault Security
- **Data-at-Rest Encryption**: `CryptoUtils` wraps standard Java Cryptography Architecture (JCA). When a session is "locked," all messages (`text`), `session_documents`, and `source_documents` are encrypted using **AES-256-GCM**.
- **Key Derivation**: Uses PBKDF2 to derive secure symmetric keys from user passwords.
- **BIP39 Seed Phrases**: Kosh supports 12-word mnemonic recovery phrases (using `BIP39Utils`). The seed phrase can reconstruct the master decryption key if the vault password is lost.
- **Zero-Knowledge Architecture**: The database stores raw ciphertexts. When a session is unlocked, the ViewModel dynamically decrypts the payload into memory, and instantly re-encrypts it upon locking.

## 4. Semantic RAG & Full-Text Search
- **Document Ingestion**: Users can attach `.pdf` and `.txt` files to a chat session. `DocumentParser` extracts raw text and chunks it into smaller, manageable paragraphs (approx. 500-1000 characters).
- **SQLite FTS4 Indexing**: Chunks are stored in the local SQLite database and indexed using `FTS4` (Full-Text Search). 
- **Retrieval Fallback Pipeline**: 
    1. First, an FTS query attempts to match user keywords.
    2. If no direct match is found, a Custom Stop-Word Filtration algorithm strips common conversational words (e.g., "what", "is", "the") and retries.
    3. If still no matches are found, it falls back to a Recency Heuristic (fetching the latest chunks of the document).
- **Document Citations**: When the LLM generates a response based on a retrieved chunk, the UI renders interactive "Source Pills" pointing to the specific document.

## 5. Text-to-Speech (TTS)
- **TtsProvider Integration**: Uses Android's native `TextToSpeech` engine.
- **Chunking Algorithm**: Long AI responses are parsed into sentences using punctuation boundaries to overcome the engine's hard 4000-character limits per queue addition.
- **Audio Focus & Lifecycle**: The TTS engine ducks background music (`USAGE_ASSISTANT`), and is strictly bound to the app's lifecycle—it instantly halts if the vault locks, the app backgrounds, or the user starts a voice dictation session.

## 6. Hybrid Web Search
- **Live Context Injection**: If the user taps the Web Search icon, `SearchProviderImpl` reaches out to the internet (duckduckgo lite / bing) via `HttpURLConnection`. 
- **Local Scraping**: It extracts the text of search results locally and feeds it directly into the LLM's system prompt as a dynamic preamble. This achieves real-time knowledge synthesis without cloud API dependencies.
