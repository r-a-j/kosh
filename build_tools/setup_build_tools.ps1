$toolsDir = "d:\Work\Testbench\temp\build_tools"
if (!(Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Force -Path $toolsDir
}

# Download urls
$ndkUrl = "https://dl.google.com/android/repository/android-ndk-r26b-windows.zip"
$cmakeUrl = "https://github.com/Kitware/CMake/releases/download/v3.29.3/cmake-3.29.3-windows-x86_64.zip"

$ndkZip = "$toolsDir\ndk.zip"
$cmakeZip = "$toolsDir\cmake.zip"

Write-Host "Downloading Android NDK (r26b)..."
Invoke-WebRequest -Uri $ndkUrl -OutFile $ndkZip

Write-Host "Downloading CMake (3.29.3)..."
Invoke-WebRequest -Uri $cmakeUrl -OutFile $cmakeZip

Write-Host "Extracting CMake..."
Expand-Archive -Path $cmakeZip -DestinationPath "$toolsDir\cmake_temp" -Force
$cmakeExtractedDir = Get-ChildItem -Path "$toolsDir\cmake_temp" -Directory | Select-Object -First 1
Move-Item -Path $cmakeExtractedDir.FullName -Destination "$toolsDir\cmake" -Force
Remove-Item -Path "$toolsDir\cmake_temp" -Recurse -Force

Write-Host "Extracting Android NDK..."
Expand-Archive -Path $ndkZip -DestinationPath $toolsDir -Force

Write-Host "Setting up Ninja..."
Expand-Archive -Path "d:\Work\Testbench\temp\ninja-win.zip" -DestinationPath "$toolsDir\ninja" -Force

# Clean up zip files
Remove-Item -Path $ndkZip -Force
Remove-Item -Path $cmakeZip -Force

Write-Host "Build tools setup completed successfully!"
