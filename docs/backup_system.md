# Kosh Backup System

This document outlines the behavior and architecture of the Kosh Backup & Restore mechanism.

## Full Vault Backup
When exporting the Kosh vault, the application extracts the entire `kosh_vault.db` SQLite file.

### Dynamic Naming
To ensure backups are identifiable and to prevent accidental overwrites, the exported backup file is dynamically named by appending the current app version and the precise timestamp.
**Format**: `kosh_v<AppVersion>_<YYYYMMDD>_<HHMMSS>.kosh`
*Example*: `kosh_v1.1.0_20260523_143000.kosh`

### Security
Backups are encrypted at rest. Before exporting the `.kosh` file, the `CryptoUtils` class generates a secure PBKDF2 derivative from the user's provided password and wraps the entire `.db` file using AES-256-GCM.

### Restore Mechanism
When importing a `.kosh` backup:
1. The user inputs their backup password.
2. The payload is temporarily decrypted.
3. **Seamless Merge (`ATTACH DATABASE`)**: Instead of deleting your active database, Kosh executes `ATTACH DATABASE` to bridge the active vault and the decrypted backup.
4. It performs native `INSERT OR IGNORE` operations across `sessions`, `messages`, `checklist_states`, and `session_documents`.
5. Because Kosh utilizes `AFTER INSERT` SQLite Triggers, any imported documents are automatically and instantly indexed by the FTS4 engine during this sync process!
6. The temporary backup is securely detached and shredded, and the UI dynamically refreshes to display the merged chats alongside your existing ones.

### Biometric Invalidation Post-Restore
When restoring a backup across different devices or after an app reinstall, the Android Keystore's biometric keys (`kosh_biometric_key`) become permanently invalidated. 
If a user attempts to unlock a chat with biometrics using an invalidated Keystore key, Kosh catches the `InvalidKeyException` and explicitly prompts the user via a Toast message: *"Biometric key invalidated by restore. Please unlock with password."* This gracefully forces the user back to the primary password authentication layer, ensuring no permanent lockouts occur.
