$cmakePath = "d:\Work\Testbench\temp\build_tools\cmake\bin\cmake.exe"
$ndkPath = "d:\Work\Testbench\temp\build_tools\android-ndk-r26b"
$flatcDir = "d:\Work\Testbench\temp\build_tools\flatc"
$qnnSdkRoot = "C:\Users\raj24\Downloads\v2.46.0.260424\qairt\2.46.0.260424"

# Create a build directory
$buildDir = "d:\Work\Testbench\temp\LiteRT\build-android"
if (!(Test-Path $buildDir)) {
    New-Item -ItemType Directory -Force -Path $buildDir
}

# Environment variables
$env:ANDROID_NDK_HOME = $ndkPath
$env:QNN_SDK_ROOT = $qnnSdkRoot
$env:PATH = "d:\Work\Testbench\temp\build_tools\ninja;d:\Work\Testbench\temp\build_tools\flatc;" + $env:PATH

# Delete cached variables to force re-evaluation of flatc
$cacheFile = "$buildDir\CMakeCache.txt"
if (Test-Path $cacheFile) {
    Write-Host "Removing CMakeCache.txt..."
    Remove-Item -Force $cacheFile
}

# Execute CMake configure
$cmakeArgs = @(
    "-S", "d:\Work\Testbench\temp\LiteRT\litert",
    "-B", $buildDir,
    "-G", "Ninja",
    "-DCMAKE_MAKE_PROGRAM=d:\Work\Testbench\temp\build_tools\ninja\ninja.exe",
    "-DCMAKE_TOOLCHAIN_FILE=$ndkPath\build\cmake\android.toolchain.cmake",
    "-DANDROID_ABI=arm64-v8a",
    "-DANDROID_PLATFORM=latest",
    "-DTFLITE_HOST_TOOLS_DIR=$flatcDir",
    "-DFLATC_EXECUTABLE=d:\Work\Testbench\temp\build_tools\flatc\flatc.exe",
    "-DQAIRT_HEADERS_DIR=d:\Work\Testbench\temp\LiteRT\build-android\_deps\qnn_headers\qairt\2.44.0.260225\include\QNN",
    "-DLITECORE_HEADERS_DIR=d:\Work\Testbench\temp\LiteRT\build-android\_deps\litecore_headers\exynos-ai-litecore-v1.1.0\include",
    "-DLITERT_ENABLE_NPU=ON",
    "-DLITERT_ENABLE_GPU=OFF",
    "-DQNN_SDK_ROOT=$qnnSdkRoot",
    "-DANDROID_STL=c++_static",
    "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384",
    "-DCMAKE_BUILD_TYPE=Release"
)

Write-Host "Running CMake configure..."
& $cmakePath $cmakeArgs
