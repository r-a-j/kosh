# Qualcomm Hexagon NPU Hardware Acceleration Setup

This document provides a highly detailed guide on the architecture, compilation, integration, and verification of local hardware-accelerated LLM inference in Kosh using the Qualcomm Hexagon NPU (HTP V79 DSP) on Snapdragon 8 Elite (SM8750) chipsets (e.g., Samsung Galaxy S25 Ultra).

---

## 1. Core Architecture Overview

Local neural execution is powered by **LiteRT** (formerly TensorFlow Lite for Microcontrollers / Google AI Edge stable runtime). To achieve maximum performance and low thermal profiles, inference is offloaded from the CPU/GPU to the Hexagon HTP NPU using Qualcomm's Unified AI SDK (**QNN**).

The integration requires a chain of dynamic libraries to link the high-level Kotlin code with the Hexagon hardware:

```
[Kosh Kotlin App]
       │ (JNI Interface)
[liblitertlm_jni.so] (LiteRT-LM Java bindings)
       │ (C API calls)
[libLiteRt.so] (Core LiteRT runtime with custom Static STL symbols)
       │ (Dynamic link / dlopen)
[libLiteRtDispatch_Qualcomm.so] (Qualcomm Vendor Dispatch Bridge)
       │ (QNN API calls)
[libQnnHtp.so] & [libQnnSystem.so] (QNN Driver stub interfaces)
       │ (Qualcomm Hexagon IPC / FastRPC)
[libcdsprpc.so] (Android System CDSP RPC driver)
       │ (DSP Instruction execution)
[libQnnHtpV79Skel.so] (Hexagon V79 DSP firmware library)
```

---

## 2. Dynamic Library Components

The following native libraries must be packaged in Kosh's JNI directory (`app/src/main/jniLibs/arm64-v8a/`):

| Library Filename | Source / Origin | Purpose |
| :--- | :--- | :--- |
| `libLiteRt.so` | Custom compiled from `google-ai-edge/LiteRT` | Core LiteRT runtime. Rebuilt locally with static STL to define key RTTI standard library symbols globally. |
| `libLiteRtDispatch_Qualcomm.so` | Compiled from `google-ai-edge/LiteRT` | Implements the LiteRT Dispatch interface. Bridges LiteRT execution graph with Qualcomm's QNN framework. |
| `libQnnHtp.so` | Qualcomm AI Engine SDK | Entry point interface for QNN Hexagon Tensor Processor (HTP) backend. |
| `libQnnSystem.so` | Qualcomm AI Engine SDK | Evaluates system properties, versions, and capability profiles of the NPU backend. |
| `libQnnHtpV79Skel.so` | Qualcomm AI Engine SDK | The actual DSP skeleton library uploaded to the CDSP processor to execute hardware operations. |
| `libQnnHtpV79Stub.so` | Qualcomm AI Engine SDK | CPU stub wrapper for communicating with the DSP skeleton. |

---

## 3. Linker & Compilation Specifications

### Static STL Linkage (`-DANDROID_STL=c++_static`)
By default, compiling Android JNI libraries links the standard library dynamically (`libc++_shared.so`). However, because LiteRT's Java bindings and AAR dependencies contain their own runtime namespace instances, dynamic linking causes `dlopen` symbol resolution conflicts.
Specifically, `libLiteRtDispatch_Qualcomm.so` fails to load with the error:
`cannot locate symbol "_ZTISt12length_error" (typeinfo for std::length_error)`
* **Resolution**: Rebuilt `libLiteRt.so` and `libLiteRtDispatch_Qualcomm.so` with `-DANDROID_STL=c++_static`. Statically linking the standard library compiles all STL types directly into the binary, exporting `_ZTISt12length_error` globally in our local `libLiteRt.so`.

### 16 KB Page Alignment (`-Wl,-z,max-page-size=16384`)
Modern Android versions (starting with Android 15/16) enforce 16 KB memory page alignment for ELF segments to support performance optimizations on high-end chipsets.
* **Resolution**: Configured all CMake builds with the shared linker flags `-Wl,-z,max-page-size=16384`. This aligns all program LOAD headers to `0x4000` boundaries, passing the Android OS safety checks.

### FlatBuffers Alignment (`flatc` v25.9.23)
The model execution graphs are serialized as FlatBuffers. Compilation of XNNPACK and LiteRT-LM requires version alignment of the flatc schema compiler with FlatBuffers header files fetched during dependency configurations. We aligned the schema compilation toolchain to `v25.9.23` to prevent compiler static assertion failures.

---

## 4. App Integration & Loading Pipeline

### Gradle Packaging Configuration (`useLegacyPackaging`)
Qualcomm's QNN backend stubs search for and dynamically link dependencies (such as `libQnnHtpV79Skel.so`) using physical filesystem paths inside the application's native library directory. By default, Android Gradle Plugin leaves native `.so` files compressed inside the APK.
* **Resolution**: Added `useLegacyPackaging = true` inside `app/build.gradle.kts`'s `packaging` block. This forces the Android Package Manager to decompress and extract JNI libraries to `/data/app/~~.../lib/arm64` on installation, allowing dynamic loader searches to succeed.

### JNI Packaging Precedence Override
To ensure that our custom-compiled `libLiteRt.so` (which contains the static STL typeinfo symbols) is packaged instead of the standard Maven-packaged AAR version:
* **Resolution**: Placed our custom `libLiteRt.so` directly inside `app/src/main/jniLibs/arm64-v8a/`. Gradle's build system gives local files in `jniLibs` absolute precedence over dependencies packaged inside external AAR files, overriding it during APK construction.

### Explicit Library Preloading Order
In Android's dynamic linker namespace, loading the vendor dispatch library directly does not automatically resolve core runtime functions if they have not been loaded into the class loader namespace.
* **Resolution**: Refactored `LiteRTModelProvider.kt` to load libraries manually using `System.loadLibrary` in a strict order preceding QNN initialization:
  1. `"LiteRt"` (Preloads the core runtime and registers static STL symbols like `std::length_error` in the namespace).
  2. `"QnnSystem"`
  3. `"QnnHtp"`
  4. `"LiteRtDispatch_Qualcomm"` (Loaded successfully now that all runtime symbols exist in the namespace).

---

## 5. UI Layout Constraints & Overlap Protection

To support long hardware-specific model names (e.g. `gemma-4-E2B-it_qualcomm_sm8750.litertlm`), Kosh's top bar layout uses a strictly zoned and responsive architecture:

```
┌────────────────────────────────────────────────────────┐
│ [Menu] ── Spacer(12) ── [ Center Box (Weight=1) ] ── Spacer(12) ── [Actions Row] │
│                           [GEMMA-4-E2B (NPU) v]        │ [Lock] [Temp] [+] [Set] │
└────────────────────────────────────────────────────────┘
```

1. **Spacer Safety Zones**: Explicit `Spacer(12.dp)` components separate the left menu and the right action rows from the center selector chip.
2. **Weighted Centered Box**: The core selector chip sits inside a `Box(Modifier.weight(1f), contentAlignment = Alignment.Center)`. This Box scales dynamically to occupy all remaining width and centers its content, ensuring it never overlaps or touches neighboring buttons under any screen size.
3. **Ellipsis & Truncation**: The text inside the chip uses `maxLines = 1` and `overflow = TextOverflow.Ellipsis` with a child weight constraint, guaranteeing it will gracefully display an ellipsis on tight space constraints.
4. **Descriptive Name Sanitization**: Strips verbose model parameters (e.g. `-IT` and `_QUALCOMM_SM8750`) and appends ` (NPU)` dynamically to create clean visual titles.

---

## 6. Build Tools Automation & Setup Scripts

To allow reproducibility and save development setup efforts, a set of automation scripts are kept locally under the `build_tools/` folder of the project directory. 

> [!IMPORTANT]
> To keep the remote repository clean and avoid tracking environment-specific scripts on Git, the entire `build_tools/` directory, all compiled SDK binaries (NDK, CMake, flatc, Ninja), and the `LiteRT` source code clone are ignored in [.gitignore](file:///d:/Work/Testbench/temp/.gitignore). These files remain strictly local on your developer machine.

### Script Catalog

* **[setup_build_tools.ps1](file:///d:/Work/Testbench/temp/build_tools/setup_build_tools.ps1)**:
  Downloads and extracts standard build dependencies (Android NDK r26b, CMake 3.29.3, Ninja 1.12.1) using PowerShell's WebRequest.
* **[setup_build_tools_fast.ps1](file:///d:/Work/Testbench/temp/build_tools/setup_build_tools_fast.ps1)**:
  An optimized setup script using native `curl.exe` block streams for high-speed download and decompression.
* **[configure_cmake.ps1](file:///d:/Work/Testbench/temp/build_tools/configure_cmake.ps1)**:
  PowerShell script executing the full CMake configure and build stages:
  1. Configures `Ninja` as the make generator.
  2. Maps NDK's target toolchain compilation configurations.
  3. Maps QNN header dependencies (`QAIRT_HEADERS_DIR`, `LITECORE_HEADERS_DIR`).
  4. Enforces static C++ runtime linkage (`-DANDROID_STL=c++_static`) and 16 KB segment padding flags (`-Wl,-z,max-page-size=16384`).
* **[extract_npu_steps.py](file:///d:/Work/Testbench/temp/build_tools/extract_npu_steps.py)**:
  Python utility to extract transcript compilation commands and setup steps for troubleshooting reference.

