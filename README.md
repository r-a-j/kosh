# Kosh (कोश) 🧠🛡️

> *Your Personal Cognitive Vault & Local Second Brain.*

<p align="center">
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/kosh_banner.png" width="100%" alt="Kosh Banner" />
</p>

**Kosh** (meaning *Vault* or *Treasure* in Sanskrit/Hindi) is a radically private, offline-first personal knowledge assistant. It is designed to index, process, and query your life's context, documents, and data completely on your device. 

In a world where every chatbot sends your personal thoughts to the cloud, Kosh brings the power of Artificial Intelligence directly to your phone. It runs Large Language Models (LLMs) locally, ensuring your data never leaves your hands. 

---

## 📱 Application Tour

<h3 align="center">🎨 Premium Theme Variations</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/00_landing_theme_obsidian.png" width="31%" alt="Obsidian Dark Theme" />
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/00_landing_theme_aero.png" width="31%" alt="Aero Glass Theme" />
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/00_landing_theme_sand.png" width="31%" alt="Sand Warm Theme" />
</p>
<p align="center"><i>Kosh offers distinct, premium design systems with rich glassmorphic effects (Obsidian Dark, Aero Glass, and Sand Warm) tailored to fit your style.</i></p>

<h3 align="center">💬 Conversational AI & Document RAG Flow</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/04_sample_new_chat.png" width="31%" alt="New Chat Initialization" />
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/03_sample_chat.png" width="31%" alt="Active Chat with RAG Citations" />
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/05_model_hub.png" width="31%" alt="Model Manager Dashboard" />
</p>
<p align="center"><i>From initializing a fresh vault session, interacting with local models using real-time document chunk citation widgets, to managing downloaded LLMs.</i></p>

<h3 align="center">🧠 LiteRT Model Administration</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/06_model_hub_02.png" width="48%" alt="Model Import Wizard" />
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/07_model_hub_03.png" width="48%" alt="Model Details & Backend Select" />
</p>
<p align="center"><i>Verify integrity upon model import, manage tag routing, and choose hardware acceleration backends (CPU, GPU, NPU) dynamically.</i></p>

<h3 align="center">⚙️ Preferences & System Diagnostics</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/02_settings.png" width="48%" alt="General Settings Screen" />
</p>

<details>
  <summary><b>🔍 Expand to view full scrollable Settings Dashboard</b></summary>
  <br/>
  <p align="center">
    <img src="https://raw.githubusercontent.com/r-a-j/kosh/refs/heads/main/docs/images/02_settings_02.png" width="60%" alt="Scrollable Settings" />
  </p>
</details>

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
- **Hardware-Accelerated Inference**: Kosh leverages Google's LiteRT (TensorFlow Lite) engine. By tapping natively into your device's Neural Processing Unit (NPU) and GPU, Kosh delivers lightning-fast text generation without draining your battery. See [NPU Setup & Hardware Optimization](docs/npu_setup.md) for configuration details.
- **Multi-Model Core Library**: Import and manage multiple LLM models (Gemma, Llama, Qwen, etc.). Assign specialized "Tags" (General, Coder, RAG Reader) to models, and Kosh will automatically route your prompts to the best available intelligence core. For model lifecycle management, see [Architecture & JNI Bindings](docs/architecture.md).
- **Temporary "Incognito" Vaults**: Want to ask a quick question without leaving a trace? Activate Temporary Mode to keep the conversation strictly in RAM. When you close it, it vanishes forever.

### 🛡️ Military-Grade Privacy & Cryptography
- **Biometric App Lock**: Secure the entire application behind a frosted-glass biometric prompt on startup.
- **Encrypted "Sealed" Vaults**: Lock individual chat threads. Kosh encrypts your messages and documents at rest using AES-256-GCM. See [Keystore & Resilience Architecture](docs/resilience.md) for cryptography details.
- **Zero-Knowledge Backups**: Export your entire cognitive database to a secure `.kosh` file encrypted via PBKDF2 derived passwords. Restore it on any device. See [Backup System Specs](docs/backup_system.md).
- **BIP39 Seed Phrase Recovery**: Set up a 12-word recovery mnemonic to guarantee you never lose access to your locked vaults, even if you forget your password.

### 📚 Local Document RAG (Retrieval-Augmented Generation)
- **Chat with your Documents**: Import PDFs, Markdown, and Text files directly into a chat. Kosh chunks the text and builds a semantic index using SQLite FTS5. See [Memory & History Architecture](docs/memory_and_history_architecture.md).
- **Smart Context & Citations**: Ask "What does this document say about X?" and Kosh will intelligently retrieve the exact paragraphs needed to answer your question. The UI dynamically renders **Document Citations** inside the assistant's chat bubbles, showing you exactly which files were referenced. See [Chat Execution & Generation Pipeline Flow](docs/chat_execution_flow.md).
- **Atomic Integrity**: Advanced model management ensures that your AI cores are verified for integrity during import, preventing corrupted or truncated files from cluttering your library. See [Architecture & Verification](docs/architecture.md).

### 🌐 Hybrid Web-Augmented Search
- **Live Internet Scraping**: Need real-time information? Tap the globe icon. Kosh will dynamically query Tavily or Brave Search APIs to fetch live data, then inject that context into the local offline model to give you an accurate, up-to-date answer.
- **Privacy-First API Management**: Your API keys are stored locally and encrypted. Connections are direct from your device to the search providers.

### 🎨 Premium Fluid Interface
- **Dark Glassmorphism**: A stunning, futuristic dark theme with dynamic radial glows and interactive animations.
- **Performance Dashboard**: Real-time tracking of NPU/GPU load, RAM allocation, and tokens-per-second (TPS) speed.
- **Voice Dictation & TTS**: Native Speech-to-Text for input and high-quality Text-to-Speech (TTS) for AI responses.

---

## 🛠️ Technical Architecture

- **UI Framework**: Modern declarative UI built completely in [Jetpack Compose](https://developer.android.com/jetpack/compose) following clean architecture and MVI principles. See [Architecture Design](docs/architecture.md) and [Future Roadmap](docs/future_planning_breakdown.md).
- **AI Engine**: [LiteRT (TensorFlow Lite)](https://ai.google.dev/edge/litert) running `.litertlm` or `.bin` models via native Hexagon DSPs (`libcdsprpc.so`). See [NPU Setup & Hardware Optimization](docs/npu_setup.md) and [NPU Token Limits](docs/npu_token_limit_research.md).
- **Database**: Native SQLite implementing complex `FTS5` virtual tables with custom stop-words filtration and cascading relational triggers. See [Memory & History Architecture](docs/memory_and_history_architecture.md).
- **Encryption**: AES-256-GCM encryption for chat text and document chunks when vault sessions are sealed. Keys managed by Android Keystore. See [Resilience & Cryptographic Key Management](docs/resilience.md).
- **Concurrency**: Kotlin Coroutines and asynchronous `Flow` streams for real-time token generation and UI state updates. See [Chat Execution & Generation Pipeline Flow](docs/chat_execution_flow.md).

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
