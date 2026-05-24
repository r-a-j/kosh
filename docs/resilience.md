# Kosh Edge Case & Resilience Architecture

Kosh is designed to be highly resilient against corruption, unexpected system reboots, and unhandled native crashes. To prevent death loops and catastrophic data loss, we have implemented defensive boundaries across the application.

## 1. JNI/Native Crash Sentinel

**Problem:** Native C++ libraries (like `LiteRT`) can cause uncatchable JNI detachments (`SIGSEGV` or `SIGABRT`) when fed corrupted FlatBuffers or when causing Out Of Memory (OOM) errors. These crashes terminate the JVM instantly.
**Solution:**
We use a synchronous SharedPreferences "Crash Sentinel" (`engine_crashed`).
1. Before calling `LiteRTModelProvider.initialize()`, Kosh synchronously writes `engine_crashed = true` to `neural_core_prefs` using `.commit()`.
2. Once the native engine successfully returns, the flag is cleared to `false`.
3. If Kosh crashes during the native call, the flag remains `true`. On the next app launch, Kosh detects the flag, blocks the model, clears the flag, and prompts the user with a recovery dialog (Try Again / Disable Model).

*Why this matters:* This pattern is completely agnostic to the model format (GGUF, TFLite, ONNX), future-proofing our multi-model roadmap while isolating native instability.

## 2. Secure Storage Self-Healing

**Problem:** The `neural_core_prefs.xml` file is managed by Google Tink (`EncryptedSharedPreferences`). The Android Keystore backing this encryption can spontaneously invalidate keys (e.g., if the user changes lock screen credentials). If this happens, `EncryptedSharedPreferences.create()` throws an unrecoverable `SecurityException` at startup, bricking the app.
**Solution:**
We wrap `EncryptedSharedPreferences.create()` in a lazy `try-catch`. If initialization fails:
1. We clear the standard SharedPreferences.
2. We delete the corrupted `neural_core_prefs.xml` file.
3. We re-initialize the `EncryptedSharedPreferences` with fresh keys.

*Warning:* This resets the user's API settings, but it guarantees the app can still launch. Crucially, the SQLite vault uses independent Tink encryption columns, so dropping SharedPreferences does not permanently lock the user out of their database.

## 3. Database Corruption ErrorHandler

**Problem:** `SQLiteOpenHelper` throws `SQLiteDatabaseCorruptException` if a partial write occurs (e.g., battery dies). The default Android behavior is to aggressively delete the database.
**Solution:**
Kosh implements a custom `DatabaseErrorHandler` inside `KoshDatabaseHelper`.
1. When corruption is detected, it attempts to safely copy the corrupted `.db` file to a `.corrupt` backup suffix for potential manual forensic recovery.
2. This backup copy is wrapped in a generic `try-catch` to swallow `IOException`s (which often trigger if the corruption was caused by a full disk).
3. SQLite then safely deletes and recreates the database, allowing Kosh to factory reset without a death loop.

## 4. Global Coroutine Exceptions

**Problem:** Standard network timeouts or disk I/O errors thrown inside `viewModelScope.launch(Dispatchers.IO)` will crash the entire application thread.
**Solution:**
Kosh uses a customized `safeIoDispatcher` infused with a `CoroutineExceptionHandler`.
- It catches any background exceptions, prints the stack trace, and displays a user-friendly Toast notification.
- It explicitly ignores `CancellationException` to avoid flooding logs during lifecycle destruction.
- It intentionally re-throws `OutOfMemoryError` to allow a clean crash, preventing Kosh from limping along as a frozen "zombie" application with a trashed heap.
