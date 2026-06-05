# Technical Research Report: LiteRT NPU Executor Context Limitation & Token Budgeting

## Executive Summary
During long-running sessions or detailed queries on the Qualcomm Hexagon NPU backend, Kosh encountered a native C++ runtime crash:
`ERROR: [third_party/odml/litert_lm/runtime/executor/llm_litert_npu_compiled_model_executor.cc:1500] new_step must be less than or equal to TokenCount(), got 2383 vs 2382`

This report analyzes the root cause of this crash, documents how Google's LiteRT and ODML frameworks handle context constraints on NPUs, and presents an intelligent, non-breaking solution for Kosh that prevents crashes while fully preserving context retrieval (RAG), search, and conversation history features.

---

## 1. Deep Dive into the Root Cause

### A. The NPU Static Compilation Constraint
NPUs (Neural Processing Units) like the Qualcomm Hexagon HTP require statically shaped tensors and pre-compiled execution graphs to achieve massive throughput and thermal efficiency. 
When compiling a Large Language Model (such as `Gemma-4-E2B`) for the Qualcomm QNN backend:
- The maximum sequence length (which dictates the size of the Key-Value Cache, or KV-cache) is hardcoded at compile time.
- For the `Gemma-4-E2B NPU` model profile loaded in Kosh, this limit is compiled to exactly **2382 tokens**.
- Unlike CPU or GPU backends which can dynamically allocate and scale the KV-cache at runtime up to a software limit, the NPU executor cannot handle sequence indices beyond its statically compiled capacity.

### B. The Assertion Failure Mismatch
In Kosh, the `LiteRTModelProvider.kt` initializes the LiteRT-LM engine with a hardcoded `maxNumTokens = 4096`:
```kotlin
val config = EngineConfig(
    modelPath = modelPath,
    backend = litertBackend,
    maxNumTokens = 4096, // Hardcoded for all backends
    cacheDir = context.cacheDir.absolutePath
)
```

1. **JNI Layer Execution**: Because `maxNumTokens` is set to `4096`, the LiteRT-LM orchestration loop believes it has a budget of 4096 tokens (prompt + generation) and allows the inference loop to continue generating new tokens.
2. **Qualcomm Dispatch Bridge**: When the cumulative count of prompt tokens + generated tokens reaches `2382`, the model generates a new token. To write this token, the engine calls `SetCurrentStep` inside `llm_litert_npu_compiled_model_executor.cc` with `new_step = 2383`.
3. **The Assertion**: The NPU Compiled Model Executor queries the physical model's compiled capacity `TokenCount()`, which returns `2382`. It then runs the assertion:
   ```cpp
   RET_CHECK_LE(new_step, TokenCount()); // Assert 2383 <= 2382 -> FAILURE
   ```
   Because 2383 exceeds the 2382 capacity, the execution halts with an invalid argument error, causing an unrecoverable JNI crash.

---

## 2. Industry Standard Solutions & Google's Recommendations

Google's AI Edge (LiteRT/MediaPipe) team documents the following best practices regarding context windows and hardware limitations:

1. **Sync `maxNumTokens` with Compiled Limits**:
   - The value of `maxNumTokens` in `EngineConfig` must match the physical model's compiled capacity.
   - If `maxNumTokens` is set to the correct compiled capacity (e.g. `2382`), the LiteRT-LM engine's runtime loop tracks the step index and terminates the generation loop *gracefully* when the limit is reached, returning the accumulated text and firing the `onDone()` callback instead of throwing an out-of-bounds error.

2. **Negative Space Budgeting**:
   - Because the KV-cache is a fixed resource ($C = 2382$), the prompt size ($P$) directly limits the maximum length of the generated output ($G$):
     $$P + G \le C$$
   - To prevent the model from running out of space immediately (e.g., generating only 5 tokens before hitting the limit), the application must dynamically budget the prompt size.

---

## 3. The Intelligent Mitigation Strategy

To fix this crash *without* losing existing features (such as RAG, web search, or conversation history), we will implement a **Dynamic Context Budgeting & Limit Synchronization** mechanism:

### Component A: Dynamic Engine Configuration
We will configure `maxNumTokens` in `LiteRTModelProvider.kt` based on the active backend:
- For `CPU` and `GPU`: Keep `maxNumTokens = 4096` (leveraging full context size).
- For `NPU (Qualcomm)`: Set `maxNumTokens = 2380` (aligning with the physical compiled limit of 2382, with a 2-token safety buffer).

### Component B: Dynamic Prompt Character Budgeting
In `ChatViewModel.kt`, we will pass the prompt character limit `maxContextChars` dynamically to `compileFinalPrompt` depending on the active backend:
- **NPU Backend**: Set `maxContextChars = 5500` characters (approx. 1380 tokens).
  - This leaves a guaranteed **1000 tokens** ($\approx 4000$ characters) for output generation.
- **CPU/GPU Backends**: Set `maxContextChars = 12000` characters (approx. 3000 tokens).
  - This leaves a guaranteed **1096 tokens** for output generation within the 4096 limit.

### Component C: Sliding Prompt Compaction Priority
To make sure we never lose features like attached document excerpts (RAG) or web search results when using the tighter NPU budget, `compileFinalPrompt` in `LlmUseCase.kt` will prioritize prompt elements:
1. **System Instructions & Query**: Guaranteed and untouched.
2. **Document Context (RAG)**: Up to 3 target excerpts are injected.
3. **Web Search Results**: Injected if active.
4. **Conversation History**: The sliding window is compressed dynamically. If the combined size of the prompt exceeds the budget, the history is truncated turn-by-turn. 
This ensures Kosh never drops critical files/context, but instead compresses history turns to fit the NPU's physical limits.

---

## 4. Verification Plan

1. **Unit Testing**:
   - Run existing unit tests to verify that prompt compilation handles lower character limits gracefully without crash.
   - Run tests to check that older history is truncated turn-by-turn when the budget is tight.
2. **Manual Testing on NPU**:
   - Deploy a debug build to a Samsung Galaxy S25 Ultra (or target Snapdragon 8 Elite device).
   - Initiate a session with NPU acceleration enabled, attaching a long document (triggering RAG).
   - Ask a query requiring a detailed explanation (e.g. "Explain in detail...").
   - Verify that generation runs smoothly and halts gracefully when the context length is exhausted, without triggering any native executor crash.
