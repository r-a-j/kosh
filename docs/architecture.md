# Kosh System Architecture

Kosh is an offline-first, strictly private AI cognitive vault designed to operate entirely locally on an Android device. This document outlines the core architecture and feature modules of Kosh v1.2.0.

## Core Components

- **Repository (`ChatRepository`):** Central source of truth coordinating SQLite storage, Tink encryption, search indexing, and LiteRT memory. Handles seamless cross-database merging for backup imports.
- **Provider Interfaces:**
  - `AIProvider`: Agnostic wrapper around LiteRT (`LiteRTModelProvider`) and potentially ONNX/GGUF in the future. Isolated to prevent native crashes from affecting core app stability.
  - `SearchProvider`: Defines standard web search interface returning a structured `SearchResponse` containing crawled text context and web metadata lists.
  - `SettingsProvider`: Encrypted Tink storage for Keystore items and crash sentinels.
  - `TtsProvider`: Abstraction over Android TTS.
- **Resilience Architecture:** See [resilience.md](resilience.md) for details on our Native Crash Sentinel, JNI Fault Tolerance, SQLite Error Handlers, and Global Coroutine Handlers.

## 1. UI Layer (Jetpack Compose)
- **Declarative UX**: The entire user interface is built using Jetpack Compose, emphasizing a fluid, glassmorphic dark theme (`KoshTheme`).
- **Reactive State**: The UI consumes state from `ChatViewModel` via `StateFlow` and `mutableStateOf`. 
- **Dynamic Renderers**: Uses a custom Markdown parser to format AI output. It supports live "Thinking" indicators, typing effects, and dynamically updates when chunks are streamed from the LiteRT engine.
- **Visual Web Previews & Math**: Renders rich webpage favicon icons and OpenGraph images/videos inside the web sources carousel, alongside typesetting LaTeX block expressions.

## 2. Neural Core (Local LLM)
- **LiteRT (TensorFlow Lite)**: The `LiteRTModelProvider` handles loading quantized `.litertlm` or `.bin` models into memory.
- **Hardware Acceleration**: Automatically delegates inference tasks to the Qualcomm Hexagon DSP (NPU) via `libcdsprpc.so`, or falls back to the GPU/CPU based on user preferences. For details on compilation, STL symbol linkage, 16 KB segment alignments, and preloading orders, see [npu_setup.md](npu_setup.md).
- **Context Management**: Kosh dynamically truncates conversation history using a sliding window to fit within the model's maximum context window (e.g., 2048 or 4096 tokens). The context allocation engine enforces strict negative-space checks to avoid prompt buffer overflows.
- **JNI Thread Safety**: Synchronizes initialization and model destruction loops to prevent JNI delegate token corruption (avoiding "laptop" looping bugs).

## 3. Cryptography & Vault Security
- **Data-at-Rest Encryption**: `CryptoUtils` wraps standard Java Cryptography Architecture (JCA). When a session is "locked," all messages (`text`), `session_documents`, and `source_documents` are encrypted using **AES-256-GCM**.
- **Key Derivation**: Uses PBKDF2 to derive secure symmetric keys from user passwords.
- **BIP39 Seed Phrases**: Kosh supports 12-word mnemonic recovery phrases (using `BIP39Utils`). The seed phrase can reconstruct the master decryption key if the vault password is lost.
- **Zero-Knowledge Architecture**: The database stores raw ciphertexts. When a session is unlocked, the ViewModel dynamically decrypts the payload into memory, and instantly re-encrypts it upon locking.
- **Memory Key Erasure**: Employs `DestroyableSecretKey` which overrides standard key-encoding frameworks to allow explicit zero-fill byte manipulation, preventing RAM retention of AES session keys.
- **AEAD Verification Integrity**: Decryption of backup imports executes atomically in memory using `doFinal` block operations. The AES-GCM AEAD authentication tag must verify successfully before any local database files are overwritten.

## 4. Semantic RAG & Full-Text Search
- **Document Ingestion**: Users can attach `.pdf` and `.txt` files to a chat session. `DocumentParser` extracts raw text and chunks it into smaller, manageable paragraphs (approx. 500-1000 characters).
- **SQLite FTS4 Indexing**: Chunks are stored in the local SQLite database and indexed using `FTS4` (Full-Text Search). All concurrent write operations are serialized on the database helper instance to avoid lock contentions.
- **Retrieval Fallback Pipeline**: 
    1. First, an FTS query attempts to match user keywords.
    2. If no direct match is found, a Custom Stop-Word Filtration algorithm strips common conversational words (e.g., "what", "is", "the") and retries.
    3. If still no matches are found, it falls back to a Recency Heuristic (fetching the latest chunks of the document).
- **Document Citations & References**: Citations are persisted as a serialized JSON object inside the database. A custom `ReferenceParser` extracts source documents and web references to present clean citations dynamically.

## 5. Text-to-Speech (TTS)
- **TtsProvider Integration**: Uses Android's native `TextToSpeech` engine.
- **Chunking Algorithm**: Long AI responses are parsed into sentences using punctuation boundaries to overcome the engine's hard 4000-character limits per queue addition.
- **Audio Focus & Lifecycle**: The TTS engine ducks background music (`USAGE_ASSISTANT`), and is strictly bound to the app's lifecycle—it instantly halts if the vault locks, the app backgrounds, or the user starts a voice dictation session.

## 6. Hybrid Web Search
- **Manual & Smart Intent Triggering**: Web search can be forced manually via a Globe toggle button in the input area or automatically triggered when the system detects temporal signals (`today`, `recent`, `latest`, `forecast`, `now`) or stock/weather queries.
- **Resilient Multi-UA Scraper**: `SearchProviderImpl` accesses search indexing targets using rotating desktop/mobile User-Agents and human-like request headers to bypass IP blocks.
- **Webpage Page Crawling**: Downloads webpage DOM tree structures, targets main `<article>` or `<main>` sections, parses textual context for prompts, and extracts metadata previews (OpenGraph `og:image`, `og:video`, `twitter:image`).
- **Coil Previews Carousel**: The sources section displays rich preview image cards and adds play badges for video-linked pages.

## 7. LaTeX Mathematical Typesetting
- **Block Mathematical Extraction**: Response parsers extract block-level equations bounded by `$$ ... $$` or `\[ ... \]` and segregate them into `ChatContentBlock.MathBlock` slots.
- **KaTeX WebView Composable**: Displays formulas inside a transparent WebView loaded with KaTeX CSS/JS dependencies.
- **Monospace Plaintext Fallback**: If the device is offline or the renderer fails to load within the timeout, it reverts to a monospace plain-text block wrapping the original LaTeX code to maintain readable equations.

## 8. Agent Loop & Skills Framework
- **Asynchronous Agent executor**: Coordinates multi-turn LLM reasoning loops, parsing JSON tool calls from model outputs and feeding back execution results.
- **Just-in-Time Permissions**: Bridges background execution flow with the Main thread UI Activity layer using `CompletableDeferred` to request permissions dynamically when needed.
- **Reflective Suspend Wrapper**: Invokes Kotlin suspend functions reflectively using Java reflection combined with coroutines intrinsic context (`suspendCoroutineUninterceptedOrReturn`), saving APK space.
- **Sandbox WebView & Execution Guards**: Executes untrusted JS plugins on a headless WebView with timeout bounds and limits turn count to 5 to avoid infinite loops.
- **For details, see [agent_and_skills.md](agent_and_skills.md)**.

## 9. Relational Chat Tagging System
- **SQLite Schema Relations**: Implemented in database version 6. It manages tags through a `tags` table (storing unique `id`, human-readable `name`, and custom `color` hex string) and a `session_tags` mapping table (connecting `session_id` and `tag_id` with foreign keys and cascade constraints).
- **Curated Theme Colors**: Users can assign one of 20 logical, high-contrast, theme-adaptive colors using a dynamic slider/carousel to visually differentiate sessions on the home empty-state dashboard.
- **Single-Query Session Joins**: To keep session loading efficient, `KoshDatabaseHelper` loads all `session_tags` and `tags` via a single joined query, caching them into a memory map to populate `ChatSession.tags` in a single pass.
- **Cascaded Integrity Checks**: Removing a tag queries the database for session associations first. If the association count $N > 0$, the app prompts with a warning confirmation dialog before executing the SQLite cascade deletion to clean up junction rows automatically.

## 10. Response Feedback & SQLite Persistence
- **DB Schema Upgrade (v7)**: Introduces database version 7, adding an unencrypted `feedback` `INTEGER` column to the `messages` table defaulting to `0` (Neutral).
- **Feedback Representation**: Represents thumbs-up/down ratings numerically: `0` for neutral, `1` for Helpful (Like), and `-1` for Unhelpful (Dislike).
- **Asynchronous Safe Writes**: Rating changes are immediately written to the ViewModel's local Compose state on the Main thread for zero UI latency, while the persistence update is scheduled asynchronously to SQLite on Kosh's `safeIoDispatcher`.
- **Decoupled UI state**: Removes local transient state inside the response actions row, binding feedback highlights dynamically to the database records to ensure states are retained when sessions are reloaded or the app is rebooted.

