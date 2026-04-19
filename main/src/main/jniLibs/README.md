# libbergamot-sys.so — Prebuilt native libraries

## Source

These `.so` files are built from DavidVentura's open-source Android port of the
Bergamot translator: https://github.com/DavidVentura/offline-translator

The JNI export symbols (`Java_dev_davidv_bergamot_NativeLib_*`,
`Java_dev_davidv_bergamot_LangDetect_*`) match the `dev.davidv.bergamot` package
in this repository exactly.

## Build steps

Clone the source repository and follow the build instructions in its README.
The build uses Docker and the Android NDK to produce `.so` files for each ABI.

```bash
git clone --recurse-submodules https://github.com/DavidVentura/offline-translator
cd offline-translator

# The bergamot subproject lives on the armv7-2 branch, not on master.
# master's settings.gradle.kts only contains `include(":app")`, so without
# this checkout Gradle fails with "project 'bergamot' not found in project ':app'".
git checkout armv7-2
git submodule update --init --recursive

# Build the Docker image (one-time setup, ~20-40 min)
docker build -t bergamot-builder .

# Build the native library for a specific ABI (replace x86_64 with the target ABI)
# Valid values: arm64-v8a, armeabi-v7a, x86, x86_64
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W)":/home/vagrant/build/dev.davidv.translator/ \
  bergamot-builder \
  bash -c "sed -i 's/\r//' gradlew && ./gradlew :app:bergamot:assembleRelease -Pandroid.injected.build.abi=<ABI> --no-daemon"

# Extract the .so from the resulting AAR
unzip -p app/bergamot/build/outputs/aar/bergamot-release.aar \
  jni/<ABI>/libbergamot-sys.so > libbergamot-sys.so
```

### x86_64 patch (required)

Before building for x86_64, apply the following change to
`app/bergamot/src/main/cpp/CMakeLists.txt` in the cloned repository.
Add the highlighted block after the existing `armeabi-v7a` block:

```cmake
if (ANDROID_ABI STREQUAL "armeabi-v7a")
  set(BUILD_ARCH "armv7-a" CACHE STRING "Build architecture" FORCE)
endif ()
# Add this:
if (ANDROID_ABI STREQUAL "x86_64")
  set(BUILD_ARCH "x86-64" CACHE STRING "Build architecture" FORCE)
endif ()
```

This sets the compiler target to the SSE2 baseline (no AVX2), which is required
for compatibility with all x86_64 Android emulators regardless of hypervisor.
Without this patch the build machine's native CPU features (e.g. AVX2) are used,
causing SIGILL crashes on emulators that do not expose AVX2 (WHPX, KVM).

## SHA256 checksums

```
56207d4d17547c326ceb55bffa1b249747f105b869e6c694c64f0f47c5704552  arm64-v8a/libbergamot-sys.so
93706cae725e143c1835702dded485c24308e33de1e738c5f2ed23d1a0a519d8  armeabi-v7a/libbergamot-sys.so
a1b4ba79caa1a8c4a1bf47a383ceff1a964ae25862993537f6e1772ecf33b09d  x86/libbergamot-sys.so
22c924c7391d0be1b1f5211192d1a13b368507c308318096b15a0580634c8703  x86_64/libbergamot-sys.so
```

To verify:
```
sha256sum arm64-v8a/libbergamot-sys.so armeabi-v7a/libbergamot-sys.so x86/libbergamot-sys.so x86_64/libbergamot-sys.so
```
