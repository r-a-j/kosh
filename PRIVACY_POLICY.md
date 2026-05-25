# Privacy Policy for Kosh

**Last Updated: May 23, 2026**

Kosh ("the App") is an offline-first, private personal vault application designed for offline AI interactions and data storage. We are committed to protecting your privacy. This Privacy Policy explains our practices regarding user information.

---

### 1. Information Collection & Use
Kosh is designed to run entirely locally on your device.
* **No Server Storage**: All of your chats, configurations, document indexes, database entries, and cryptographic keys are generated and stored exclusively in your device's secure local storage.
* **No Data Collection**: We do not collect, monitor, track, upload, or transmit any personal data, usage logs, search histories, or conversation text. 
* **Zero Analytics**: Kosh does not use any third-party analytics or crash-reporting libraries that transmit data off your device.

### 2. Permissions & Security Features
The App requests specific Android system permissions to provide optional security and operational features:
* **Biometric Lock (Fingerprint/Face Unlock)**: Used locally to unlock your encrypted chats and startup vault lock. Authentication is handled entirely by the Android OS Keystore system. We never access, capture, or store your biometric templates.
* **Internet Permission**: Used solely for the optional Web Search feature. When enabled, the App queries third-party providers (Tavily/Brave) directly from your device.
* **Storage Access**: Used solely to import AI models and documents into the App's secure internal vault.

### 3. Local Document Processing
When you import documents (PDF, TXT, MD) into Kosh:
* The text is extracted and indexed locally on your device to enable Retrieval-Augmented Generation (RAG).
* No document content is ever sent to any server for processing or indexing.
* Imported documents are stored in an encrypted format within the App's private storage.

### 4. Data Encryption
All sensitive data (chat history, session keys, API keys, and document chunks) is locally encrypted on your device using **AES-256-GCM**.
* Encryption keys are managed locally by the Android Keystore system.
* Your passcode is never stored; instead, a PBKDF2 derived key is verified on the fly through decryption checks.
* Backups exported by the user are encrypted using a user-specified password before being written to external storage.

### 5. Third-Party Services
If you choose to enable the Web Search feature:
* Kosh interacts with third-party APIs (Tavily or Brave).
* Your search queries are sent directly to these providers. Please refer to their respective privacy policies regarding how they handle search data.
* Kosh does not share any other personal data (like your identity or vault contents) with these services.

### 6. Children's Privacy
Because we collect absolutely zero information, Kosh is safe for all age groups, but we restrict usage according to regional app store guidelines.

### 7. Changes to This Policy
We may update our Privacy Policy from time to time. Any changes will be reflected by posting the new Privacy Policy in this repository.

### 8. Contact Us
If you have any questions or feedback about this Privacy Policy, please open an issue in the public GitHub repository: [github.com/r-a-j/kosh](https://github.com/r-a-j/kosh).
