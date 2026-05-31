# Agent Instructions for `shared/core/secure-storage`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for platform secret storage.

## Scope

Applies to:
- `shared/core/secure-storage/...`

## Local context

This module stores user-supplied credentials such as third-party API keys behind a narrow common contract. Platform actuals use native secret storage where available and documented best-effort fallback where the platform cannot provide a real vault.

## Local rules

- Keep `commonMain` to the contract and Metro DI provider. Platform APIs belong in matching actual source sets.
- Platform actuals bind each platform or platform family to the native secret vault:
  - Android — Keystore-backed AES-256-GCM, ciphertext in a private `SharedPreferences` file. This is hand-rolled because `androidx.security:security-crypto` is `@Deprecated`; the primitives it composed are stable platform APIs.
  - iOS — Keychain `SecItem*`, `kSecClassGenericPassword`, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
  - Desktop JVM — `java-keyring` for Windows Credential Vault, macOS Keychain, or Linux libsecret. When no OS keyring backend is available, `DesktopSecureStorage` falls back to a process-local in-memory store and the user re-enters their key each launch.
  - JS / Wasm — the shared `webMain` actual uses `window.localStorage`; same-origin JavaScript can read every value, so this is best-effort only.
- Reads return `null` only for "not configured"; platform errors should surface instead of silently falling through.
- Writes and deletes must fail loudly on platform errors.
- Keep secrets opaque strings. Do not add serialization or inspect secret contents here.
- Browser storage is best-effort only; do not describe JS/Wasm storage as production-grade secret protection.
- Do not add plaintext-file persistence as a fallback for missing desktop keyring support.

## Local verification

- Run the affected source-set compile/test task for platform implementation changes.
- For contract changes, run the consuming settings/profile or app-shell checks that bind secure storage.

## Do not

- Do not log, print, snapshot, or fixture real credentials.
- Do not move platform vault APIs into `commonMain`.
- Do not silently downgrade platform storage failures to "not configured".

## Related skills

- `.agents/skills/kmp-source-set-review`
- `.agents/skills/observability-logging-review`
