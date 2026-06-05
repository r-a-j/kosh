# AGENTS: How to get productive in Kosh

> [!IMPORTANT]
> **ABSOLUTE RULE FOR ALL AGENTS:** Do NOT commit or push code to GitHub without explicit permission from the user. Even if you have generated documentation or completed verification tests, you must stop, summarize your changes, and ask the user if they would like to commit and push. Never execute a commit or push command without this explicit user directive.
>
> **ABSOLUTE RULE FOR ALL AGENTS:** Do NOT write project-specific documentation or knowledge base entries to the global `.gemini` folder or global agent knowledge directories (e.g., `C:\Users\raj24\.gemini\antigravity\knowledge`). All technical specifications, architecture blueprints, execution flows, and project-specific knowledge base documents must be saved strictly under Kosh's local `docs/` folder in this repository.

This file contains concise, actionable guidance for AI coding agents working in this repository. Focus on discoverable patterns, key integration points, and exact commands.

1) Big picture (quick):
   - App: Android (Jetpack Compose UI) + local LLM inference via LiteRT (native JNI). See `docs/architecture.md`.
   - Core coordinator: `ChatViewModel` (app/src/main/.../ChatViewModel.kt) and `ChatRepository`/use-cases under `domain/usecase`.
   - Neural core: `LiteRTModelProvider` (app/src/main/java/.../LiteRTModelProvider.kt) — JNI + Engine lifecycle and backend selection.

2) Key files & why they matter (examples):
   - `app/src/main/java/.../data/LiteRTModelProvider.kt` — model initialization, manual `System.loadLibrary` order (LiteRt, QnnSystem, QnnHtp, LiteRtDispatch_Qualcomm), backends: "CPU", "GPU", "NPU (Qualcomm)". Useful when diagnosing NPU load failures.
   - `app/src/main/java/.../data/ModelLibraryManager.kt` — models stored in `files/models`; imports enforce integrity (expected size) and a 100 MB minimum. Sanitize/dedupe behavior is here.
   - `app/src/main/java/.../data/SharedPrefsSettingsProvider.kt` — EncryptedSharedPreferences wrapper and deterministic recovery path when prefs are corrupted (deletes xml then recreates). Shows keys used (e.g., `engine_crashed`).
   - `app/src/main/java/.../ui/chat/ChatViewModel.kt` — central control flow for generation, RAG retrieval, sentinel usage (`settingsProvider.commitBoolean("engine_crashed", true)`), model selection, and backend strings.
   - `app/src/main/java/.../ui/chat/components/MathFormulaCard.kt` — typesets LaTeX formulas via WebView; currently relies on external CDNs which causes failures in offline mode.
   - `docs/npu_setup.md` and `build_tools/` — exact native build & packaging notes (useLegacyPackaging, static STL, 16 KB alignment). Follow these when building native libs.

3) Short actionable developer workflows
   - Build (Windows PowerShell):
     ```powershell
     # Assemble debug APK and run unit tests
     .\gradlew.bat assembleDebug; .\gradlew.bat test
     # Run instrumentation tests (device/emulator connected)
     .\gradlew.bat connectedAndroidTest
     ```
   - Prepare native build environment (local only):
     ```powershell
     # one-time: download and extract NDK/CMake/Ninja
     pwsh build_tools\setup_build_tools.ps1
     # configure and build CMake native bits used by LiteRT (see docs/npu_setup.md)
     pwsh build_tools\configure_cmake.ps1
     ```

4) Project-specific conventions & gotchas
   - Models: treated as first-class assets under `context.filesDir/models`. Imports enforce exact byte size check (ModelLibraryManager.importModel) — tests and agents should pass expected size when simulating imports.
   - Minimum valid model size: 100 * 1024 * 1024 bytes (100MB) — small files are cleaned up automatically.
   - Backend strings are literal: use exactly "CPU", "GPU", or "NPU (Qualcomm)" when calling `aiProvider.initialize(...)`.
   - Engine crash sentinel: code writes `engine_crashed=true` synchronously before JNI initialize and clears it on return. Resilience flow in `ChatViewModel.initializeEngine()` and `docs/resilience.md` must be preserved when changing initialization logic.
   - JNI shutdown: `LiteRTModelProvider.close()` performs JNI close on a background thread to avoid deadlocks — do not force close on UI thread or you may hang the process.
   - Settings & prefs recovery: `SharedPrefsSettingsProvider` will delete corrupted `neural_core_prefs.xml` and recreate encrypted prefs. Avoid relying on encrypted prefs for irreversible state.
   - LaTeX mathematical equations: Rendered inside a transparent WebView loaded with KaTeX. It has a known limitation of using online CDN links which fail when offline. Local assets bundle implementation is planned.
   - Chat scroll list ordering: `ChatScreen.kt` message list uses `reverseLayout = false` in its `LazyColumn`. Scrolling is controlled by custom viewport-relative scroll helper extensions (`scrollToBottom` and `animateScrollToBottom`) to maintain scroll stability during token generation.
   - Scroll Lock & Resumption Heuristics: User drag gestures are monitored using `collectIsDraggedAsState()`. If a user scrolls up during generation, `userHasScrolledUp` becomes true, which pauses auto-scrolling so they can read history without viewport drift. Auto-scrolling resumes as soon as they scroll back to the bottom (where `isAtBottom` resets the flag).
   - Token & Database Write Throttling: To prevent recomposition lag and SQLite I/O bottlenecks, tokens received from JNI are buffered and flushed to the Main thread at most once every 32ms (~30fps) or on newlines. Saves of the active response stream to SQLite are throttled to once every 500ms during active generation, with a final write on completion.
   - Assistant Message Placeholder Lifecycle: To eliminate bubble completion flashing and Compose re-entrance animations, the ViewModel instantly inserts an assistant placeholder message (`ChatMessage` with `isStreaming = true`) at startup and updates its contents inline.
   - Journal protection: The tag named "Journal" (case-insensitive checking in view model) is protected and cannot be created, edited, deleted, or disassociated. All new journal entry chat sessions automatically start with this tag allocated.
   - Session List Caching Gotcha: The session lists on the Dashboard and Journal Vault screens are filtered/remembered. To ensure updates (like deletes, renames, or lock/unlock status changes) are reflected immediately in Compose, the `remember` blocks must key on `viewModel.savedSessions.toList()` instead of `viewModel.savedSessions` directly.
   - Deletion Security: Session deletion on all interfaces (Drawer, Dashboard, Journal list) must use `DeleteSessionDialog` to enforce biometric/passcode-secured deletion for encrypted chats. When a session is deleted, call `activeSessionKeys.remove(sessionId)` to prevent key leaks.
   - Low Battery Warning: A warning banner appears when the battery is under 20% (and not charging), and a warning dialog prevents sending messages when the battery is under 15% (requiring explicit confirmation to proceed).
   - RAG Memory Consolidation: A background consolidation task (`updateRAGMemoryConsolidation`) extracts facts, formulas, and context, updating a consolidated `SessionDocument` (with `chunkIndex = -2`) to optimize offline context.
   - Qualcomm NPU Budgeting: When using the "NPU (Qualcomm)" backend, the context limit is set to 5500 characters (leaving space for generation within the 2382 token hardware limit). Other backends use up to 12000 characters.

5) Integration points & external dependencies to watch
   - Native libs and JNI: `app/src/main/jniLibs/arm64-v8a/` (local JNI takes precedence); see `docs/npu_setup.md` for packaging rules (useLegacyPackaging=true).
   - Search integrations: `SearchProvider` implementations call external scrapers/APIs. Keys stored in settings: `tavily_api_key`, `brave_api_key`.
   - Crypto: `CryptoUtils`, Tink/EncryptedSharedPreferences, and Android Keystore are used; key invalidation and recovery flows are in `docs/resilience.md`.

6) Fast debugging tips (concrete)
   - If model init fails with UnsatisfiedLinkError: check `LiteRTModelProvider` manual library load order and confirm `.so` files exist in `jniLibs/arm64-v8a` and are uncompressed in APK (useLegacyPackaging).
   - If EncryptedSharedPreferences throws SecurityException at startup: inspect `SharedPrefsSettingsProvider` recovery path — tests can simulate corrupted prefs by creating a broken `neural_core_prefs.xml` file.
   - To reproduce crash-sentinel behavior: call `ChatViewModel.initializeEngine()` and kill process during native init to leave `engine_crashed=true`; on next launch `ChatViewModel` will show recovery dialog.

7) Where tests live & how they exercise code
   - Unit tests: `app/src/test/java/...` (e.g., `ChatViewModelTest.kt`) use fakes for `AIProvider`, `ModelLibraryManager`, and `SettingsProvider`. Use `.\gradlew.bat test` to run.

8) Useful search anchors for agents
   - Search for: `engine_crashed`, `LiteRTModelProvider`, `ModelLibraryManager`, `ModelTag`, `SharedPrefsSettingsProvider`, `useLegacyPackaging`, `libLiteRt`, `packaging`, `MathFormulaCard`, `reverseLayout`, `future_planning_breakdown.md`, `updateRAGMemoryConsolidation`, `DeleteSessionDialog`.

9) Local Documentation Index (in `docs/` directory)
   - [architecture.md](file:///d:/Work/Testbench/temp/docs/architecture.md) — High-level architecture (Android Compose + LiteRT native JNI loading).
   - [chat_execution_flow.md](file:///d:/Work/Testbench/temp/docs/chat_execution_flow.md) — Detailed step-by-step chat pipeline and model invocation flow.
   - [npu_setup.md](file:///d:/Work/Testbench/temp/docs/npu_setup.md) — NPU build details (16 KB page alignment, CMake configurations, packaging).
   - [resilience.md](file:///d:/Work/Testbench/temp/docs/resilience.md) — Resilience, EncryptedSharedPreferences recovery, and Keystore setup.
   - [future_planning_breakdown.md](file:///d:/Work/Testbench/temp/docs/future_planning_breakdown.md) — Roadmap notes (LaTeX mathematical formulas, local translation, GPS, battery warning details).
   - [backup_system.md](file:///d:/Work/Testbench/temp/docs/backup_system.md) — Detailed description of the backup/import/export system.
   - [memory_and_history_architecture.md](file:///d:/Work/Testbench/temp/docs/memory_and_history_architecture.md) — Details of the chat history database and context management.
   - [agent_and_skills.md](file:///d:/Work/Testbench/temp/docs/agent_and_skills.md) — Details on the Agent execution loop and registered capabilities.
   - [npu_token_limit_research.md](file:///d:/Work/Testbench/temp/docs/npu_token_limit_research.md) — Context limit optimizations specifically for the Qualcomm NPU.
   - [on_device_tts_analysis.md](file:///d:/Work/Testbench/temp/docs/on_device_tts_analysis.md) — Text-to-speech engine and offline model analysis.

Keep entries above factual and discoverable; update this file when core flows change.

