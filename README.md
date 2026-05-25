# Kosh (कोश) 🧠🛡️

> *Your Personal Cognitive Vault & Local Second Brain.*

**Kosh** (meaning *Vault* or *Treasure* in Sanskrit/Hindi) is a radically private, offline-first personal knowledge assistant. It is designed to index, process, and query your life's context, documents, and data completely on your device. 

In a world where every chatbot sends your personal thoughts to the cloud, Kosh brings the power of Artificial Intelligence directly to your phone. It runs Large Language Models (LLMs) locally, ensuring your data never leaves your hands. 

---

## 🌟 Why Kosh? (Use Cases)

**The Privacy-Conscious Professional**
You have sensitive work documents, proprietary code snippets, or confidential project plans. You can't risk uploading them to ChatGPT or Claude. With Kosh, you can attach PDFs and text files directly into an encrypted "Sealed Vault". The local AI reads and summarizes them entirely offline.

**The Frequent Traveler & Offline Researcher**
Stuck on a 12-hour flight without Wi-Fi? Kosh’s neural core runs directly on your phone's NPU/GPU. You can continue brainstorming, querying your saved documents, and generating ideas without ever needing a cellular connection.

**The Secure Journaler**
You want to use AI to reflect on your daily life, but your thoughts are strictly personal. You can lock specific chat threads in Kosh using your fingerprint or a custom password. If you hand your phone to a friend, your "Sealed Vaults" remain cryptographically locked and invisible.

---

## ✨ Core Features

### 🧠 True Offline-First Local AI
- **Hardware-Accelerated Inference**: Kosh leverages Google's LiteRT (TensorFlow Lite) engine. By tapping natively into your device's Neural Processing Unit (NPU) and GPU, Kosh delivers lightning-fast text generation without draining your battery.
- **Multi-Model Core Library**: Import and manage multiple LLM models (Gemma, Llama, Qwen, etc.). Assign specialized "Tags" (General, Coder, RAG Reader) to models, and Kosh will automatically route your prompts to the best available intelligence core.
- **Temporary "Incognito" Vaults**: Want to ask a quick question without leaving a trace? Activate Temporary Mode to keep the conversation strictly in RAM. When you close it, it vanishes forever.

### 🛡️ Military-Grade Privacy & Cryptography
- **Biometric App Lock**: Secure the entire application behind a frosted-glass biometric prompt on startup.
- **Encrypted "Sealed" Vaults**: Lock individual chat threads. Kosh encrypts your messages and documents at rest using AES-256-GCM. 
- **Zero-Knowledge Backups**: Export your entire cognitive database to a secure `.kosh` file encrypted via PBKDF2 derived passwords. Restore it on any device.
- **BIP39 Seed Phrase Recovery**: Set up a 12-word recovery mnemonic to guarantee you never lose access to your locked vaults, even if you forget your password.

### 📚 Local Document RAG (Retrieval-Augmented Generation)
- **Chat with your Documents**: Import PDFs, Markdown, and Text files directly into a chat. Kosh chunks the text and builds a semantic index using SQLite FTS5.
- **Smart Context & Citations**: Ask "What does this document say about X?" and Kosh will intelligently retrieve the exact paragraphs needed to answer your question. The UI dynamically renders **Document Citations** inside the assistant's chat bubbles, showing you exactly which files were referenced.
- **Atomic Integrity**: Advanced model management ensures that your AI cores are verified for integrity during import, preventing corrupted or truncated files from cluttering your library.

### 🌐 Hybrid Web-Augmented Search
- **Live Internet Scraping**: Need real-time information? Tap the globe icon. Kosh will dynamically query Tavily or Brave Search APIs to fetch live data, then inject that context into the local offline model to give you an accurate, up-to-date answer.
- **Privacy-First API Management**: Your API keys are stored locally and encrypted. Connections are direct from your device to the search providers.

### 🎨 Premium Fluid Interface
- **Dark Glassmorphism**: A stunning, futuristic dark theme with dynamic radial glows and interactive animations.
- **Performance Dashboard**: Real-time tracking of NPU/GPU load, RAM allocation, and tokens-per-second (TPS) speed.
- **Voice Dictation & TTS**: Native Speech-to-Text for input and high-quality Text-to-Speech (TTS) for AI responses.

---

## 🛠️ Technical Architecture

- **UI Framework**: Modern declarative UI built completely in [Jetpack Compose](https://developer.android.com/jetpack/compose).
- **AI Engine**: [LiteRT (TensorFlow Lite)](https://ai.google.dev/edge/litert) running `.litertlm` or `.bin` models via native Hexagon DSPs (`libcdsprpc.so`).
- **Database**: Native SQLite implementing complex `FTS5` virtual tables with custom stop-words filtration and cascading relational triggers.
- **Encryption**: AES-256-GCM encryption for chat text and document chunks when vault sessions are sealed. Keys managed by Android Keystore.
- **Concurrency**: Kotlin Coroutines and asynchronous `Flow` streams for real-time token generation and UI state updates.

---

## 🚀 Getting Started

### Prerequisites
- An Android device running Android 15 (API level 36) or above is recommended for full NPU support.
- Devices with dedicated NPUs (e.g., Snapdragon 8 Gen series, Google Tensor) are highly recommended.

### Setup & Installation

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/r-a-j/kosh.git
   cd kosh
   ```

2. **Load Your Core Model**:
   - Obtain a compatible LiteRT LLM model file (e.g., Gemma 2B or Llama 3.2).
   - Launch the application and use the **Neural Core Wizard** or **Cognitive Library** to import the model file into your secure vault.

3. **Configure & Ignite**:
   - Select your hardware backend (NPU, GPU, or CPU).
   - Tap **IGNITE NEURAL ENGINE** to synchronize cognitive pathways and begin chatting with your second brain.

---

## 🔒 The Kosh Promise
**No Telemetry. No Analytics. No Cloud Servers.** 
Your thoughts, your documents, and your data belong to you. Kosh is built to ensure it stays that way.
