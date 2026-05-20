# shared/core/secure-storage

Platform secret storage for user-supplied credentials (third-party API keys).
The contract is intentionally narrow: a string by typed key, read/write/delete,
suspend at the boundary because every actual is backed by a system call or
async IO.

## Invariants

- `commonMain` carries the contract and a Metro `@Provides`. No platform code.
- Actuals bind each platform or platform family to the native secret vault:
  - Android — Keystore-backed AES-256-GCM, ciphertext in a private
    `SharedPreferences` file. Hand-rolled because
    `androidx.security:security-crypto` is `@Deprecated`; the primitives it
    composed (Keystore master key + AES/GCM) are stable platform APIs.
  - iOS — Keychain `SecItem*`, `kSecClassGenericPassword`,
    `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
  - Desktop JVM — `java-keyring` (Windows Credential Vault / macOS Keychain /
    Linux libsecret). Linux without libsecret degrades to a documented
    plaintext-file fallback.
  - JS / Wasm — a shared `webMain` actual backed by `window.localStorage`.
    The browser security model makes this best-effort: same-origin JS can read
    every value. Documented at the boundary; the demo build is not intended for
    production secrets.
- Reads return `null` for "not configured" but **throw** on actual platform
  errors. Writes always throw on failure. Silent fallthrough hides crypto/OS
  problems from the user; the AI/TTS seam already handles missing-key as a
  first-class state.
- No serialization beyond `String`; secrets are opaque to this module.
