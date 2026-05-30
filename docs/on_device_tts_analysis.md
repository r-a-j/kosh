# On-Device TTS Analysis for Kosh
## Offline-First, Privacy-Preserving Text-to-Speech Strategy

**Date:** May 2026  
**Context:** Kosh is privacy-first, offline-first. No external API calls (except optional web search). TTS must run entirely on-device or on a local companion service (user-controlled).

---

## 1. Candidate Models & Feasibility Assessment

### Summary Table: On-Device TTS Models

| Model | Weights (FP32) | Quantized (INT8) | Latency (CPU / GPU) | Convertible? | On-Device Verdict |
|-------|---|---|---|---|---|
| **Coqui VITS (small)** | ~100–150 MB | ~25–40 MB | 1–5s / 200–500ms | ✅ ONNX / TFLite | 🟡 Marginal (needs GPU) |
| **FastSpeech2 + MelGAN** | ~150–200 MB | ~40–60 MB | 2–8s / 300–800ms | ✅ ONNX / TFLite | 🟡 Marginal (needs GPU) |
| **Glow-TTS + HiFi-GAN** | ~200–300 MB | ~50–80 MB | 3–10s / 500–1500ms | ⚠️ Partial (HiFi-GAN tricky) | 🔴 Difficult (large, slow) |
| **Bark (Suno)** | ~1–3 GB (full) | ~300–600 MB | 5–30s / 2–5s | 🔴 No (JAX/PyTorch complex) | 🔴 Not viable |
| **Tortoise TTS** | ~1–2 GB | ~300–500 MB | 10–60s / 5–15s | 🔴 No (complex autoregressive) | 🔴 Not viable |
| **Android Platform TTS** | 0 (built-in) | N/A | 500–2000ms / N/A | N/A (native) | 🟢 Always available (poor quality) |
| **EdgeTTS / FastPitch** | ~80–120 MB | ~20–30 MB | 1–4s / 150–400ms | ✅ ONNX / TFLite | 🟢 Promising (experimental) |
| **MeloTTS (simplified)** | ~100 MB | ~25 MB | 1–3s / 150–350ms | ✅ ONNX | 🟢 Promising (new, less tested) |

**Key:** 🟢 = Viable, 🟡 = Possible but constrained, 🔴 = Not practical.

---

## 2. Detailed Model Deep-Dive

### A. Coqui VITS + MelGAN / HiFi-GAN
- **Status:** Stable, widely used, research-backed.
- **Quality:** Good naturalness; decent accent/voice control via speaker ID or reference audio.
- **Weight Sizes:**
  - VITS acoustic model: ~80–150 MB (FP32) → ~20–40 MB (INT8).
  - MelGAN vocoder: ~10–20 MB (FP32) → ~3–5 MB (INT8).
  - HiFi-GAN vocoder (higher quality): ~50–100 MB (FP32) → ~15–25 MB (INT8).
- **Inference Latency (on typical GPU like Snapdragon Adreno):**
  - Acoustic: 200–800ms for a ~2s audio clip (text duration-dependent).
  - Vocoder: 100–400ms.
  - **Total end-to-end:** 300–1200ms (reasonable for async TTS).
- **Conversion Path:**
  - PyTorch (native) → ONNX → TFLite (via ONNX → TFLite converter).
  - Intermediate: use `torch.onnx.export()` and `onnx-tf` or direct TFLite from ONNX.
  - Status: established, used by community; some hurdles with dynamic shapes.
- **On-Device Feasibility:** 🟡 **Marginal.** With quantization + GPU/NPU offload, can work but:
  - Requires GPU/NPU support for real-time responsiveness; CPU-only is 5–10s per message (too slow).
  - Model footprint after quantization (~40–60 MB combined) is acceptable.
  - Conversion pipeline is established but non-trivial (1–2 weeks engineering for first time).
- **Recommendation for POC:** Start here after validating quality locally.

### B. FastSpeech2 + MelGAN
- **Status:** Research-standard; pre-trained models available (Coqui, TensorFlow).
- **Quality:** Similar to VITS; good controllability via acoustic feature conditioning.
- **Latencies:** Similar to VITS (slightly faster acoustic, similar vocoder).
- **Conversion:** ✅ ONNX/TFLite supported; slightly easier than Glow-TTS.
- **On-Device Feasibility:** 🟡 **Marginal.** Same constraints as VITS; slightly simpler conversion.

### C. Bark (Suno.ai)
- **Status:** State-of-the-art zero-shot, expressive TTS; very flexible voice/style control.
- **Quality:** Excellent; one of the best for naturalness and style transfer.
- **Weight:** ~1–3 GB (full stack, includes encoders and decoders).
- **Issues:**
  - Built in JAX + custom ops; no straightforward ONNX export.
  - Latency on mobile: 5–30s per message CPU, 2–5s GPU minimum.
  - Model size even quantized is 300–600 MB; feasible but very large.
- **On-Device Feasibility:** 🔴 **Not practical for initial POC.** Conversion is complex; would require significant effort. Revisit only if on-device TTS becomes critical and you have resources.

### D. EdgeTTS / FastPitch (Emerging)
- **Status:** Newer, designed for lower-resource inference (edge devices).
- **Quality:** Good, competitive; less research maturity than VITS.
- **Weights:** ~80–120 MB (FP32) → ~20–30 MB (INT8).
- **Latency:** 150–400ms GPU, 1–4s CPU.
- **Conversion:** ✅ ONNX-native; very straightforward.
- **On-Device Feasibility:** 🟢 **Promising.** Designed for this use case; conversion is simpler. Less community test data; some risk.

### E. MeloTTS (Emerging, experimental)
- **Status:** Recent entrant to the TTS landscape; designed for efficiency.
- **Quality:** Decent; less extensive evaluation than VITS.
- **Weights:** ~100 MB (FP32) → ~25 MB (INT8).
- **Latency:** 150–350ms GPU, 1–3s CPU.
- **Conversion:** ✅ ONNX.
- **On-Device Feasibility:** 🟢 **Promising.** Worth prototyping; similar profile to EdgeTTS. Risk: newer, smaller community.

### F. Android Native TextToSpeech (Built-in)
- **Status:** Available on all Android devices; no download needed.
- **Quality:** Poor naturalness (robotic, limited prosody); limited accent/voice options.
- **Latency:** 500–2000ms total (fast).
- **Weights:** 0 (already in ROM).
- **On-Device Feasibility:** 🟢 **Always available as fallback.** Not sufficient for "good experience" but good for offline emergency playback.

---

## 3. Realistic On-Device TTS for Kosh

### Option A: Hybrid Approach (Recommended)
**Strategy:** Default to Android platform TTS (fallback) + ship a lighter neural TTS model (Coqui VITS small quantized) as an optional user download.

**Implementation:**
1. **Immediate (no changes required):** Use existing Android TTS. Quality is poor, but it's 100% guaranteed offline and low-latency.
2. **Phase 1 (2–4 weeks):** Integrate Coqui VITS + MelGAN (quantized) as an optional `LocalNeuralTtsProvider`:
   - Package model weights as downloadable optional asset (~40 MB).
   - Convert to TFLite and bundle with JNI wrapper for NNAPI/GPU acceleration.
   - Add UI toggle: "Use Neural TTS" (if model downloaded) or fallback to Android TTS.
3. **Inference at runtime:** call native code (similar to LiteRT pipeline) to run the model; stream or buffer audio.

**Pros:**
- Immediate improvement: Android TTS works today (robotic but usable).
- Gradual upgrade path: users can download neural model for better quality.
- Preserves offline guarantees.

**Cons:**
- Model packaging complexity similar to LiteRT (JNI, native libraries, quantization).
- Users interested in high quality must download ~40 MB and have GPU/NPU support.
- Development effort: 2–4 weeks for first POC.

**Timeline & Effort:**
- Research + prototype model conversion: 1–2 weeks.
- JNI wrapper + NNAPI/GPU fallback: 1–2 weeks.
- Testing + optimization: 1 week.

### Option B: Lightweight Local Companion Service (For Power Users)
**Strategy:** If on-device is too constrained, allow users to run a small TTS service on a local desktop/server (on their own network) and configure Kosh to call it (localhost only, no internet exposure).

**Implementation:**
1. Document a simple TTS microservice (Python + FastAPI) that users can run on their own machine: `tools/tts_companion_service/`.
2. Add `LocalCompanionTtsProvider` to Kosh that connects to `http://localhost:PORT`.
3. User opts-in by running the companion app and configuring URL in Kosh.

**Pros:**
- Offloads computation to desktop (better latency + quality).
- Users maintain full control and privacy (localhost only).
- Minimal Android app engineering (just HTTP client).

**Cons:**
- Requires user to run and maintain a separate service (friction).
- Not realistic for typical end users; better for power users / self-hosted enthusiasts.
- Not truly "just Kosh" without the companion.

**Timeline & Effort:**
- Write companion service: 1 week.
- Add provider + settings UI: 3–5 days.

### Option C: Pre-Computed Voice Dataset (Compromise)
**Strategy:** Synthesize and cache high-quality TTS audio offline for common responses / templated outputs.

**Implementation:**
1. Identify the most common types of assistant responses (summaries, citations, explanations, etc.).
2. Pre-synthesize using a high-quality model (Bark/Tortoise on a server, one-time batch job).
3. Store MP3/WAV versions of common phrases/templates in the app.
4. At runtime, reconstruct response audio from cached pieces (stream concatenation) or fall back to Android TTS for new/rare text.

**Pros:**
- Very fast playback (cached audio; no inference delay).
- High quality (pre-synthesized offline).
- No runtime inference; minimal overhead.

**Cons:**
- Does not support arbitrary text (only templated responses).
- Large asset bundle if covering many common responses.
- Fragile: does not generalize; if user receives unexpected text, quality degrades.
- Not a real TTS solution; more of a band-aid.

**Timeline & Effort:**
- Identify templates: 1 week.
- Batch synthesize: 1–2 days.
- App integration (cache + playback): 3–5 days.

---

## 4. Recommended Strategy for Kosh (Phased)

### Short-term (weeks 0–4)
- **Do nothing:** Keep using Android platform TTS.
- **Measure:** Collect user feedback on TTS quality; determine if it's a major blocker.
- **Research:** Run small conversion experiment (take a Coqui VITS small model locally, attempt ONNX → TFLite conversion on your dev machine).

### Medium-term (weeks 4–12) — If TTS quality is a priority
- **Pick Coqui VITS + MelGAN approach (Option A, Phase 1):**
  1. Convert small Coqui VITS model to ONNX locally (1 week research + experiments).
  2. Convert ONNX to TFLite (with quantization pipeline) — 1 week.
  3. Build JNI wrapper + NNAPI delegate support (similar to LiteRT model integration) — 2 weeks.
  4. Add UI: toggle for neural TTS, optional model download (1 week).
  5. Test on real devices with GPU/NPU support (Snapdragon 8 variants) — 1 week.

- **Alternatively, use companion service (Option B, for power users):**
  1. Build Python FastAPI wrapper around Coqui VITS (1 week).
  2. Add Kosh provider + settings UI (1 week).
  3. Package as Docker/executable for easy deployment.

- **Or, quick hybrid (Option A + B fallback):**
  1. Ship Android TTS by default.
  2. Allow power users to self-host companion service.
  3. Start conversion experiments in parallel (lower priority).

### Long-term (months 3+) — If adoption is strong
- Scale on-device model support: test multiple models, optimize NNAPI performance, add speaker cloning.
- Explore on-device voice cloning (speaker embedding insertion).

---

## 5. Model Weights & Conversion Paths (Technical Reference)

### Coqui VITS Small (Recommended for POC)

**Source:** https://github.com/coqui-ai/TTS (models/en/vctk/vits)

**Weights (approximate):**
- Acoustic model (VITS): ~100–150 MB
- Vocoder (MelGAN): ~10–20 MB
- **Total FP32:** ~120–170 MB
- **Total INT8 quantized:** ~30–50 MB

**Conversion Pipeline:**
```
PyTorch (.pth) 
  ↓ (torch.onnx.export)
ONNX (.onnx)
  ↓ (tensorflow-onnx or onnx2tf)
TensorFlow SavedModel (.pb)
  ↓ (TFLite converter)
TensorFlow Lite (.tflite)
  ↓ (quantize if needed)
TFLite INT8 (.tflite, int8)
```

**Time Estimate:** 1–2 weeks of work first time (learning curve + debugging).

**Known Hurdles:**
- VITS uses some dynamic torch ops that don't export cleanly to ONNX; may need manual graph rewrites.
- MelGAN includes transposed convolutions; ensure TFLite supports the ops.
- Quantization of vocoder requires calibration data; test on real audio samples.

### FastSpeech2 + MelGAN (Alternative, simpler conversion)
- Similar weights; conversion slightly easier (no dynamic shapes in FastSpeech2).
- Same vocoder vocoder bottleneck.

### EdgeTTS / MeloTTS (Experimental, easier conversion)
- Already ONNX-native; direct → TFLite path.
- Less community experience; start with Coqui for safety unless you want to pioneer.

---

## 6. Hardware Requirements & Feasibility on Android Devices

### Typical Snapdragon Chipsets (2024-2026)

| Chipset | GPU Cores | NPU | Typical Max VRAM | Feasible? |
|---------|-----------|-----|---|---|
| Snapdragon 8 Elite (SM8750) | 12× Adreno | ✅ Hexagon | 12 GB | ✅ YES (ideal) |
| Snapdragon 8 Gen 3 (SM8650) | 8× Adreno | ✅ Hexagon | 8–12 GB | ✅ YES |
| Snapdragon 8 Gen 2 (SM8550) | 6× Adreno | ✅ Hexagon | 8 GB | ✅ YES (marginal) |
| Snapdragon 7+ (SM7475) | 3× Adreno | ⚠️ Hexagon | 8 GB | 🟡 MAYBE (CPU-only slow) |
| Google Tensor / Tensor 4 | Mali GPU | ✅ TPU | 8–12 GB | ✅ YES (TPU support varies) |
| Samsung Exynos 2500 | Mali GPU | Varies | 8–12 GB | 🟡 MAYBE |
| Qualcomm 6x series (mid-range) | 1–2 Adreno | No | 6–8 GB | 🔴 NO (CPU-only, too slow) |

**Conclusion:**
- On **flagship Android phones (Snapdragon 8 Gen 2+, Tensor, Exynos premium):** Coqui VITS + MelGAN quantized can run at reasonable latency (200–800ms, GPU accelerated).
- On **mid-range phones:** CPU-only inference is 5–10s per message; too slow for responsive UX.
- On **low-end phones:** Not practical.

**Strategy:** Tier support: offer neural TTS on-device only for premium devices; fallback to Android TTS or companion service for others.

---

## 7. Development Roadmap & Resource Estimates

### Phase 1: Proof-of-Concept (2–3 weeks, 1 engineer)
- **Goal:** Validate model conversion and on-device performance.
- **Tasks:**
  1. Local experiment: download Coqui VITS small, synthesize text locally (PyTorch).
  2. Convert to ONNX and TFLite locally; measure model sizes and conversions.
  3. Write a small CLI tool or Jupyter notebook to test inference time.
  4. Benchmark quantization (INT8) and acceptable quality loss.
- **Output:** Conversion pipeline documentation + empirical metrics (model size, latency).
- **Decision point:** If acceptable, proceed to Phase 2. If too heavy, pivot to hybrid or companion service.

### Phase 2: Android Integration (3–4 weeks, 2 engineers)
- **Goal:** Integrate model into Kosh as optional download + JNI provider.
- **Tasks:**
  1. Create JNI wrapper around TFLite inference (similar to LiteRT setup; reuse patterns from `LiteRTModelProvider.kt`).
  2. Add NNAPI delegate support for GPU/NPU acceleration.
  3. Implement `LocalNeuralTtsProvider` class (follows `TtsProvider` interface).
  4. Add model download + cache logic (download to `files/tts_models/`).
  5. Add UI toggle in settings to enable/disable neural TTS.
- **Output:** Working on-device neural TTS provider; model download flow.
- **Testing:** Device testing on Snapdragon 8 Elite, Tensor, and mid-range phones (performance tiers).

### Phase 3: Optimization & Production (2–3 weeks, 2 engineers)
- **Goal:** Performance tuning, caching, error handling.
- **Tasks:**
  1. Cache synthesized audio (key: hash of text + voice params); skip re-synthesis for repeated text.
  2. Add robustness: fallback to Android TTS if neural TTS fails or times out.
  3. Handle edge cases: long text (split and concatenate), streaming audio playback.
  4. Profile and optimize model quantization further if needed.
  5. Write unit + integration tests (fakes + real model on CI where feasible).
- **Output:** Production-ready on-device TTS with caching and resilience.

### **Total effort for on-device neural TTS:** ~6–8 weeks, 2–3 engineers.

---

## 8. Alternative: Companion Service (Power Users)

### Lightweight Python TTS Service

If full on-device integration is too heavy, provide a simple companion service (Python + FastAPI) that power users can run:

**Service structure:**
```
tools/tts_companion_service/
├── main.py                 # FastAPI server
├── requirements.txt        # TTS dependencies
├── Dockerfile              # For easy deployment
├── README.md               # Setup instructions
└── models/                 # (downloaded at first run)
```

**Service API (minimal):**
- POST /synthesize { text, accent?, speed? } → returns audio/wav
- POST /health → status

**Setup (for user):**
```powershell
# 1. Clone / download
git clone <repo> && cd tools/tts_companion_service

# 2. Setup (one-time)
python -m venv venv
./venv/Scripts/activate
pip install -r requirements.txt
python main.py  # downloads models (~100 MB first run)

# 3. Configure Kosh: Settings → TTS → "Use Local Service" → http://localhost:8000
```

**Effort:** ~1 week to write and package.

---

## 9. Recommendation Summary

| Approach | Quality | Effort | User Friction | Privacy | Recommended? |
|----------|---------|--------|---|---|---|
| Android TTS (status quo) | Poor (robotic) | 0 | None | ✅ Full | 🟡 Fallback only |
| Coqui VITS on-device (Phase 1) | Good | 6–8 wks | Download model (~40 MB) | ✅ Full | 🟢 **YES** if timeline allows |
| Companion service (power users) | Good | 1 wk | Run separate app | ✅ Full (localhost) | 🟡 Secondary option |
| Pre-computed cache | Excellent | 2 wks | None (templated) | ✅ Full | 🔴 Not a solution (limited) |
| External API (HF, etc.) | Excellent | 1 day | None | ❌ Privacy breach | 🔴 **NO** (against Kosh principles) |

**Final Recommendation for Kosh:**
1. **Short-term:** Keep Android TTS (works, but poor quality).
2. **Medium-term:** Invest in Coqui VITS + MelGAN conversion and on-device integration (6–8 weeks; major quality improvement).
3. **Alternative (if timeline is tight):** Offer companion service for power users (1 week); document as "desktop companion for better TTS quality".
4. **Avoid:** External APIs (violates privacy promise).

---

## 10. Next Steps

### Immediate Actions (this week)
- [ ] Run local POC: download Coqui VITS, synthesize a sample, measure latency.
- [ ] Attempt ONNX export and TFLite conversion; document any blockers.
- [ ] Share results with team to validate feasibility and timeline.

### If on-device is green-lit (4–12 weeks)
- [ ] Start Phase 1 (conversion pipeline POC).
- [ ] Plan JNI integration (reuse LiteRT patterns).
- [ ] Allocate 2–3 engineers.

### If timeline is too tight
- [ ] Build companion service skeleton (1 week).
- [ ] Document for power users / self-hosted enthusiasts.
- [ ] Plan on-device roadmap for later.

---

## Appendix A: Model Sources & Links

- **Coqui TTS:** https://github.com/coqui-ai/TTS
- **Bark:** https://github.com/suno-ai/bark
- **Tortoise:** https://github.com/neonbjb/tortoise-tts
- **Hugging Face TTS Models:** https://huggingface.co/models?pipeline_tag=text-to-speech
- **ONNX Model Zoo:** https://github.com/onnx/models
- **TensorFlow Lite:** https://www.tensorflow.org/lite
- **Android NNAPI:** https://developer.android.com/ndk/guides/neuralnetworks

---

## Appendix B: Quantization & Performance Notes

### INT8 Quantization Effects
- **Size reduction:** ~3–4x (e.g., 100 MB → 25–30 MB).
- **Latency impact:** typically 10–30% faster (with NNAPI/GPU) due to smaller memory footprint.
- **Quality loss:** minimal for TTS acoustic models; listen to samples before committing.

### GPU/NPU Acceleration
- **NNAPI delegat:** Preferred on Android; offloads compatible ops to GPU/NPU/DSP automatically.
- **TFLite GPU delegate:** Alternative if NNAPI not available; fallback to CPU.
- **Snapdragon NPU (Hexagon):** Very well suited for audio inference; similar to LiteRT LLM support.

---


