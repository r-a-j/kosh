# The DNA of Claude Code: An Architectural Deep Dive

Claude Code is Anthropic's official agentic command-line interface (CLI) for Claude. Under the hood, it is a highly sophisticated, state-of-the-art terminal application built on **Node.js/Bun**, **React Ink**, and a suite of native C++ integrations. 

This document provides a comprehensive analysis of Claude Code's codebase structure, lifecycle, agentic coordinator loop, tool system, sandboxing mechanics, memory subsystems, and advanced features (teleportation, voice mode, and LSP).

---

## 1. Codebase Anatomy & Directory Map

The `src/` directory contains 35 subdirectories and 18 files. Below is a map of the core components:

*   **`assistant/` & `coordinator/`**: Manage long-running, autonomous agent loops. `coordinator/coordinatorMode.ts` contains the system prompts, worker delegation specs, and progress tracking for parallel subagents.
*   **`bootstrap/`**: Tracks global runtime state (e.g., active session details, current working directory, allowed settings, non-interactive flags).
*   **`bridge/`**: Implements the local-machine bridge used by the CLI to act as a remote host in SSH or remote-control mode.
*   **`cli/`**: Houses update checkers, exit handlers, print pipelines, and ndjson/structured IO transports.
*   **`components/`**: Standard React Ink components for rendering fullscreen layouts, file diffs, log selectors, model pickers, onboarding flows, status lines, and text inputs.
*   **`entrypoints/`**: Houses the main runtime entrypoints (`cli.tsx` for bootstrapping, `init.ts` for environment setup, and `mcp.ts` / `sandboxTypes.ts`).
*   **`memdir/`**: Implements the durable, file-based memory system (read/write/search of memories and shared team memory).
*   **`migrations/`**: Code migrators that execute updates to settings, keys, and model strings across version upgrades.
*   **`plugins/`**: Registry and management for built-in plugins (which package skills, hooks, and MCP servers).
*   **`services/`**: Underpins advanced operations, including API communication, analytics, rate limiting, Language Server Protocol (LSP) integration, Model Context Protocol (MCP) clients, and streaming voice speech-to-text.
*   **`skills/`**: Houses pre-defined workflow/prompt bundles (e.g., `/remember`, `/skillify`, `/verify`) that direct Claude to perform structured multi-step tasks.
*   **`state/`**: The React state store (`store.ts`, `AppStateStore.ts`, `onChangeAppState.ts`) backing the TUI.
*   **`tools/`**: Concrete tool implementations (Bash, FileEdit, FileRead, WebSearch, Agent, etc.).
*   **`utils/`**: Core utilities wrapping git, process spawning, file operations, telemetry, proxies, mTLS, and TMUX.
*   **`vim/`**: Emulation layer for Vim inputs (motions, operators, text objects).
*   **`voice/`**: Simple entry state for voice dictation.

---

## 2. Bootstrapping & Lifecycle Management

The CLI initializes via `src/entrypoints/cli.tsx` and delegates system-level configurations to `src/entrypoints/init.ts`.

```mermaid
graph TD
    A[cli.tsx Entry] --> B{Special Flags?}
    B -- --version --> C[Print version & Exit]
    B -- --computer-use-mcp / --chrome-native-host --> D[Run custom server]
    B -- daemon / ps / logs / attach / kill --> E[Background Session Manager]
    B -- ssh / assistant --> F[Pre-extract args]
    B -- No Flags --> G[Load init.ts & main.tsx]
    G --> H[enableConfigs & applySafeConfigEnvVars]
    H --> I[setupGracefulShutdown]
    I --> J[Configure mTLS & Proxies]
    J --> K[Preconnect to Anthropic API]
    K --> L[Accept Trust Dialog?]
    L -- Yes --> M[Apply Full EnvVars & Init OpenTelemetry]
    L -- No --> N[Run limited / Skip hooks]
    M --> O[Render React Ink App]
```

### Key Phases:
1.  **Early Flag Parsing**: Flag checks like `--version` or `--dump-system-prompt` run instantly with zero module imports to maximize responsiveness.
2.  **Config & Environment Safe-Zone**: `applySafeConfigEnvironmentVariables()` only loads non-dangerous environment variables before the workspace trust dialog has been completed.
3.  **Network Warming**: `preconnectAnthropicApi()` fires a background TCP/TLS handshake to Anthropic's endpoints concurrently with the rest of the startup path, saving ~100–200ms on the first API call.
4.  **Graceful Teardown**: `GracefulShutdown` registers cleanup tasks (such as shutting down LSP servers, deleting temporary git worktrees, or cleaning session-specific swarm teams).

---

## 3. Interactive REPL & UI Architecture

Claude Code's terminal user interface is powered by **React Ink**, which transforms React component trees into terminal-compatible ANSI text.

### Key UI Capabilities:
*   **Vim Mode Emulation (`src/vim/`)**: Implements a complete Vim engine inside the TUI. It handles movements (`h`, `j`, `k`, `l`, `w`, `e`, `$`, `0`), text objects (`aw`, `ap`, `i"`), operators (`d`, `c`, `y`), and mode transitions (Normal vs. Insert).
*   **Virtual Message List (`VirtualMessageList.tsx`)**: Renders very long conversation transcripts smoothly in the terminal by only rendering elements within the visible viewport.
*   **Interactive Diffs (`FileEditToolDiff.tsx`)**: Employs structured line diffs that let the user preview, reject, or edit proposed code changes inline before committing.
*   **Diagnostics Display (`DiagnosticsDisplay.tsx`)**: Connects to the LSP engine to show inline syntax, compiler, or lint warnings/errors.

---

## 4. Agentic Coordinator & Subagents

A major highlight of Claude Code is its **Coordinator Mode** (`src/coordinator/coordinatorMode.ts`), which allows Claude to act as a project manager spawning autonomous workers/subagents.

```
                  ┌───────────────────────┐
                  │   User Request        │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │   Coordinator Agent   │
                  │   (Claude Code)       │
                  └─────┬───────────┬─────┘
                        │           │
       Spawn (Parallel) │           │ Spawn
                        ▼           ▼
                  ┌───────────┐ ┌───────────┐
                  │ Worker A  │ │ Worker B  │
                  │ (Explore) │ │ (Research)│
                  └─────┬─────┘ └─────┬─────┘
                        │             │
        XML Notification│             │ XML Notification
                        ▼             ▼
                  ┌───────────────────────┐
                  │   Coordinator Agent   │
                  │   (Synthesize Spec)   │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │ Worker C (Implement)  │
                  └───────────────────────┘
```

### Delegation Rules:
1.  **Parallel Execution**: The coordinator is instructed to spawn independent workers in parallel (e.g., doing code exploration and test-suite research simultaneously) by making multiple `AgentTool` calls in a single message.
2.  **XML Task Notifications**: Subagent execution results are delivered back as user-role messages containing `<task-notification>` XML tags, carrying `task-id`, `status`, `summary`, `result`, and token `usage`.
3.  **Synthesis vs. Delegation**: The coordinator is prohibited from lazy delegation (e.g., writing "Based on your findings, fix the bug"). It must read worker findings, synthesize the actual code changes (files, line numbers, variable updates), and compile a specific instruction spec.
4.  **Context-Overlap Management**: When a worker finishes research, the coordinator decides whether to **Continue** it via `SendMessage` (reusing its loaded context if the work overlaps closely) or **Spawn Fresh** (avoiding context noise).

---

## 5. Robust Tool System

Claude Code relies on strict tool definitions built with a Zod schema compiler (`src/Tool.ts`). 

### Tool execution features:
*   **Pre-Read Invariant**: The `FileEditTool` enforces a strict rule: the model **must** read a file using `FileReadTool` before editing it. If the file has been modified on disk by the user or a linter since it was read, the edit fails with `FILE_UNEXPECTEDLY_MODIFIED_ERROR`, forcing the model to read it again.
*   **Atomic replacement**: `FileEditTool` performs exact string replacements. It validates that the target `old_string` is unique in the file to prevent false replacements, unless `replace_all` is set.
*   **LSP hooks**: On successful file writes, the CLI triggers didChange/didSave notifications to active LSP instances (e.g., the TypeScript compiler service) to refresh diagnostic registers.

---

## 6. Security, Metacharacter Verification & Sandboxing

Command execution inside `BashTool` is heavily secured. If sandboxing is enabled, commands run inside an isolated OS container governed by `@anthropic-ai/sandbox-runtime` (utilizing **bubblewrap** on Linux and sandboxing profiles on macOS).

### Metacharacter & Command Validation (`bashSecurity.ts`, `readOnlyValidation.ts`):
*   **Command Substitution Blocks**: Actively blocks `$()`, backticks, Zsh process substitutions (`<()`, `>()`, `=()`), equals expansions (`=cmd`), and glob qualifiers (`(e:)`, `(+)`) that can execute code in arguments.
*   **Safe Heredocs**: Integrates a custom line-by-line parser to identify safe heredoc substitutions like `$(cat <<'EOF'\n...\nEOF)`. It verifies that the delimiter is single-quoted or escaped (preventing shell expansion inside the body) and blocks nested delimiters or trailing command injections.
*   **Git Commit Gate**: Intercepts `git commit -m "..."` to skip full validation checks for simple commit messages, but bails to strict validators if it detects backslashes, unquoted redirections (`<`, `>`), or chained operators.
*   **Read-Only Allowlist**: For speculative, non-interactive execution (e.g. testing or research), a strict allowlist of commands and flags is enforced:
    *   *`xargs`*: Removes `-i` and `-e` because GNU getopt optional-arg parsing (`i::`, `e::`) can cause argument-consumption mismatches, allowing arbitrary binary execution.
    *   *`fd`*: Removes `-x`, `-X`, and `-l` (which runs `ls` and presents path-hijacking risks).
    *   *`ps`*: Blocks BSD-style `e` option to prevent environment variable dumps.
    *   *`tree`*: Blocks `-R` because it writes `00Tree.html` files to subdirectories (unauthorized write).
    *   *`date`*: Blocks `-s`/`--set` and `-f`/`--file` and requires positional arguments to start with `+` to prevent setting system time.
*   **Bare-Git Repo Escape Protection**: Git's `is_git_directory()` treats any directory as a bare repo if it finds files like `HEAD`, `objects/`, or `refs/`. An attacker could plant these in a project directory along with a `config` file containing a custom `core.fsmonitor` script. When an unsandboxed `git` command runs, it would execute the malicious script. Claude Code mitigates this by denying writes to these filenames in the sandbox and running a clean scrub (`scrubBareGitRepoFiles`) to delete them if they are created.

---

## 7. Persistent Memory System

Claude Code employs a persistent, file-based memory system located under `~/.claude/projects/<slug>/memory/`.

```
                    ┌────────────────────────────┐
                    │       ~/.claude/...        │
                    │      Memory Directory      │
                    └─────────────┬──────────────┘
                                  │
                   ┌──────────────┴──────────────┐
                   ▼                             ▼
       ┌───────────────────────┐     ┌───────────────────────┐
       │       MEMORY.md       │     │    topic_files.md     │
       │    Distilled Index    │     │   (user_role.md, etc) │
       │  (200-line/25KB cap)  │     │   Detailed Memories   │
       └───────────────────────┘     └───────────────────────┘
```

### The Taxonomy (`memoryTypes.ts`):
Memories are constrained to four types capturing context **not** derivable from code, git history, or CLAUDE.md:
1.  **`user`**: User's role, goals, and expertise.
2.  **`feedback`**: Guidelines given to Claude (both what to avoid and what worked). Leads with the rule, followed by a **Why:** and **How to apply:**.
3.  **`project`**: Ongoing initiatives, deadlines, or decisions.
4.  **`reference`**: Pointers to external systems (Linear, Grafana, Slack).

### Memory Lifecycle:
*   **Standard Writing**: A 2-step process where the model writes detailed facts to a dedicated markdown file (including frontmatter name, description, and type) and appends a single-line pointer (`-[Title](file.md) - hook`) to `MEMORY.md`.
*   **Background Session Extraction (`extractMemories.ts`)**: Runs at the end of each query loop. A forked subagent analyzes the session transcript, extracts new memories, writes them to the memory folder, and appends a notice to the user.
*   **Auto-Dream Consolidation (`autoDream.ts`)**: A background consolidation task. When more than 24 hours and 5 sessions have passed since the last consolidation, a subagent is spawned to review recent logs, clean up duplicates, resolve conflicts, and distill findings into updated topic files and a clean `MEMORY.md`.

---

## 8. Advanced Integrations

### A. Voice Mode (`src/services/voice.ts`)
*   **Audio Capture**: Links against a native Rust/C++ module (`audio-capture-napi`) which uses `cpal` to record audio on macOS, Linux, and Windows. If the native module is unavailable on Linux, it falls back to SoX (`rec`) or `arecord` (ALSA).
*   **Streaming STT**: Streams raw 16kHz linear16 PCM audio in binary WebSocket frames to Anthropic's speech-to-text API. Control messages (`KeepAlive`, `CloseStream`) manage the stream.
*   **Language & Keyterms**: Supports language selection and vocabulary boosting by passing custom keyterms (extracted from context, code, or settings) to the STT model to improve transcription accuracy.

### B. Language Server Protocol (LSP) (`src/services/lsp/`)
*   **Plugin-Managed**: Spawns language servers configured through plugins (like TypeScript, Python, or Go language servers) in the background.
*   **Passive Diagnostics**: Monitors open files, tracks diagnostics, and feeds compiler/lint issues back to the model as context.

### C. Teleportation & Remote Sandboxes (`src/utils/teleport.tsx`)
*   **Session Resumption**: Synchronizes conversation logs via OAuth to continue local sessions seamlessly on other machines.
*   **Seed Bundling**: If a remote environment (CCR - Claude Code Remote) is launched, the CLI stashes local changes, runs `git bundle --all`, uploads the bundle via the Files API, and instructs the remote environment to clone from this bundle. This transfers exact uncommitted local states to a remote sandboxed container without forcing git commits or pushes.

### D. Hooks Engine (`src/utils/hooks.ts`)
*   **Triggers**: Hook commands are executed at key lifecycle stages (e.g. `PreToolUse`, `PostToolUse`, `Setup`).
*   **JSON Interventions**: Hooks can return JSON instructions to block execution (`decision: block`), change tool arguments (`updatedInput`), or provide additional context. Trust rules ensure hooks are completely bypassed in untrusted directories.

---

## 9. Key Findings & Design Insights

1.  **Strict State Recovery**: The codebase contains deep mechanisms (`conversationRecovery`, `fileHistory`, `SharedPrefsSettingsProvider`) to guarantee that local state, file changes, and credentials survive crashes, keystore corruptions, and abrupt terminations.
2.  **Performance via Deferred Work**: Startup timing is optimized by deferring heavy module loading (like OpenTelemetry or React UI components) and prefetching system contexts/networks in parallel background threads.
3.  **Adversarial Security Mindset**: The validation code is written with a strict security model, blocking obscure shell features (Zsh modules, process substitutions, empty positional overrides) and sanitizing paths before execution.
4.  **Taxonomy Enforcement**: By strictly defining what is a memory, a plan, or a task, Claude Code prevents prompt bloat and keeps context sizes lean.
