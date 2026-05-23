# Kosh (कोश) 🧠🛡️
> *The Personal Cognitive Vault & Local Second Brain.*

**Kosh** (meaning *Vault* or *Treasure* in Sanskrit/Hindi) is a personal, central vector knowledge graph and local AI interface designed to securely index, process, and query your life's context, preferences, and data completely offline. 

Built with an offline-first philosophy, Kosh ensures your personal information is stored locally and secured on-device, bypassing third-party cloud servers to provide true cognitive privacy.

---

## ✨ Features

- **Offline-First Local AI**: Run local Large Language Models (LLMs) on-device utilizing the Google LiteRT (TensorFlow Lite) engine.
- **Hardware-Accelerated Inference**: Native integration with local Neural Processing Units (NPUs) and GPUs to deliver lightning-fast on-device text generation.
- **Local Document RAG Vaults**: Import PDFs, TXTs, and MDs (up to 10MB) directly into SQLite FTS4 databases for secure semantic offline context retrieval.
- **Internet-Augmented Inquiries**: Dynamic RAM-based web scraping fallback pipelines utilizing DuckDuckGo, Google, and Bing to inject live web context into local models.
- **Biometric App Lock & Encrypted Vaults**: Secure the app on startup and lock individual chat threads with passwords or fingerprints, encrypting the database using AES-256-GCM.
- **Zero-Knowledge Encrypted Backups**: Export and import your entire cognitive vault securely using PBKDF2 derived passwords.
- **BIP39 Seed Phrase Recovery**: Setup 12-word mnemonic recovery phrases for absolute secure vault restoration.
- **Speech-to-Text Integration**: Native voice dictation utilizing device speech recognition for quick inputs.
- **Premium Fluid Interface**: Designed with a futuristic dark-glassmorphism aesthetic, top-heavy radial glows, interactive UI animations, and animated canvas loading screens.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a modern, fluid declarative UI.
- **Local Engine**: [LiteRT (TensorFlow Lite)](https://ai.google.dev/edge/litert) for running optimized on-device cognitive models.
- **NPU Integration**: Native Hexagon NPU (`libcdsprpc.so`) and GPU delegate support for hardware-accelerated local execution.
- **Language**: Kotlin with structured concurrency via Coroutines and Flow.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- An Android device running Android 11 (API level 30) or above (NPU/GPU acceleration recommended)

### Setup & Installation

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/r-a-j/kosh.git
   cd kosh
   ```

2. **Load Your Core Model**:
   - Obtain a compatible LiteRT LLM model file (`.litertlm` or `.bin`).
   - Launch the application and use the **Neural Core Wizard** to select and copy the model file into the application's secure internal storage.

3. **Configure & Ignite**:
   - Select your hardware backend (NPU, GPU, or CPU).
   - Tap **IGNITE NEURAL CORE** to synchronize cognitive pathways and begin chatting with your second brain.

---

## 🔒 Privacy & Security

Everything in **Kosh** is designed to remain under your absolute control:
- **No Cloud Uploads**: Your chat history, model files, and database vectors are stored strictly inside the app's sandboxed local storage.
- **Encrypted Local Storage**: Context database is fully encrypted and stored locally.
- **No Analytics/Tracking**: Bypasses telemetry trackers to ensure absolute privacy for your personal second brain.
