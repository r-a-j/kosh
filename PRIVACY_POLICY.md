# Privacy Policy for Kosh

**Last Updated: May 23, 2026**

Kosh ("the App") is an offline-first, private personal vault application designed for offline AI interactions and data storage. We are committed to protecting your privacy. This Privacy Policy explains our practices regarding user information.

---

### 1. Information Collection & Use
Kosh is designed to run entirely locally on your device.
* **No Server Storage**: All of your chats, configurations, database entries, and cryptographic keys are generated and stored exclusively in your device's secure local storage.
* **No Data Collection**: We do not collect, monitor, track, upload, or transmit any personal data, usage logs, search histories, or conversation text. 
* **Zero Analytics**: Kosh does not use any third-party analytics or crash-reporting libraries that transmit data off your device.

### 2. Permissions & Security Features
The App requests specific Android system permissions to provide optional security and operational features:
* **Biometric Lock (Fingerprint/Face Unlock)**: Used locally to unlock your encrypted chats and startup vault lock. Authentication is handled entirely by the Android OS Keystore system. We never access, capture, or store your biometric templates.
* **Internet Permission**: Used solely to scrape web search engine results when the user explicitly requests or triggers the Web Search/RAG assistant. All connections are made directly from your device to the search provider.

### 3. Data Encryption
All sensitive data (chat history, session keys, and titles) is locally encrypted on your device using **AES-256-GCM**.
* Encryption keys are managed locally by the Android Keystore system.
* Your passcode is never stored; instead, a PBKDF2 derived key is verified on the fly through decryption checks.
* Backups exported by the user are encrypted using a user-specified password before being written to external storage.

### 4. Children's Privacy
Because we collect absolutely zero information, Kosh is safe for all age groups, but we restrict usage according to regional app store guidelines.

### 5. Changes to This Policy
We may update our Privacy Policy from time to time. Any changes will be reflected by posting the new Privacy Policy in this repository.

### 6. Contact Us
If you have any questions or feedback about this Privacy Policy, please open an issue in the public GitHub repository: [github.com/r-a-j/kosh](https://github.com/r-a-j/kosh).
