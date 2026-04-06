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

## SHA256 checksums

```
56207d4d17547c326ceb55bffa1b249747f105b869e6c694c64f0f47c5704552  arm64-v8a/libbergamot-sys.so
93706cae725e143c1835702dded485c24308e33de1e738c5f2ed23d1a0a519d8  armeabi-v7a/libbergamot-sys.so
a1b4ba79caa1a8c4a1bf47a383ceff1a964ae25862993537f6e1772ecf33b09d  x86/libbergamot-sys.so
9a33b23f96058280839a34cbfbd1a3a01c78ef01ccc8dd5ebc03021baa98bf6e  x86_64/libbergamot-sys.so
```

To verify:
```
sha256sum arm64-v8a/libbergamot-sys.so armeabi-v7a/libbergamot-sys.so x86/libbergamot-sys.so x86_64/libbergamot-sys.so
```
