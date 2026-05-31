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
   - Search for: `engine_crashed`, `LiteRTModelProvider`, `ModelLibraryManager`, `ModelTag`, `SharedPrefsSettingsProvider`, `useLegacyPackaging`, `libLiteRt`, `packaging`.

Keep entries above factual and discoverable; update this file when core flows change.

