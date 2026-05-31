# Kosh Memory & History Architecture (Knowledge Base)

## Overview
This entry documents the design decisions, implementation details, and patterns established for managing long-term conversation history, reasoning block rendering, and background context compilation in Kosh.

---

## 1. Context Window Challenges & Solutions

On-device inference with mobile LLMs (e.g. Gemma 2B via LiteRT) has rigid hardware boundaries:
*   **VRAM Limits**: Exceeding the context window (~2048 tokens or ~8000 characters) leads to critical memory pressure, high pre-fill delays, or app OOM terminations.
*   **Attention Contamination**: Models get confused when historical assistant responses containing raw thinking tags (`<thinking>` or ````thinking````) are fed back into their inputs, leading to recursive thinking loops or format breakage.

### Solutions:
*   **Dynamic Context Truncation**: When prompt content exceeds budget limits, Kosh trims oldest context blocks with a trailing `"... [truncated]"` message rather than discarding the entire conversation history context.
*   **Thinking Trace Stripping**: Assistant messages stored in history are passed through `ResponseParser.extractThinkingSegments` to completely strip reasoning blocks before prompt assembly.

---

## 2. Multi-Tiered Memory Model

Kosh splits memory into three distinct, dynamically-assembled layers:

| Layer | Type | Mechanism |
| :--- | :--- | :--- |
| **Short-Term** | Sliding Verbatim Window | Strict, verbatim sliding window of the last $N=8$ messages to maintain instant conversational continuity. |
| **Mid-Term** | Rolling Summaries | Asynchronous rolling summarization of historical messages beyond the sliding window, appended as `### CONVERSATION SUMMARY`. |
| **Long-Term** | Semantic RAG & Facts | - **Semantic turns**: Cosine-similarity overlap searches historical turns for keyword relevance and injects top-2 results.<br/>- **Fact Store**: SQLite key-value facts extracted in the background injected as `### EXTRACTED USER FACTS`. |

---

## 3. SQLite Database Schema Upgrade (v8)

The sessions table requires dedicated persistent fields to manage memory layers:
*   **`summary`**: Stores the running encrypted Rolling Summary.
*   **`facts`**: Stores the running encrypted User Facts list.

### Database Migrations
*   Increment `DATABASE_VERSION = 8` in `KoshDatabaseHelper.kt`.
*   SQL Upgrade step:
    ```sql
    ALTER TABLE sessions ADD COLUMN summary TEXT;
    ALTER TABLE sessions ADD COLUMN facts TEXT;
    ```
*   **Cryptographic Mapping**: If the active chat session is encrypted, both `summary` and `facts` columns are encrypted on-the-fly (`CryptoUtils.encryptMessage`) in `ChatSessionUseCase.saveSessionEncrypted` and decrypted in `decryptSession` before UI consumption.

---

## 4. Background Workers & Prompt Templates

Memory maintenance runs asynchronously after each model response finishes to avoid UI freeze and save compute power.

### Prompts

#### A. Fact Extraction
Runs after each turn to extract declarative user properties:
```text
Extract any key user preferences, facts, or context from the following interaction. Keep it extremely brief and format as a bulleted list.
Interaction:
User: {last_user_msg}
Assistant: {clean_assistant_msg}
Extracted facts:
```

#### B. Rolling Summaries
Runs when conversation exceeds 10 turns:
```text
Update the following running summary of the conversation to incorporate these older messages. Keep the summary highly condensed and focus on the topics discussed and decisions made.
Current Summary: {current_summary}
Older Messages:
{older_history_text}
New Summary:
```
