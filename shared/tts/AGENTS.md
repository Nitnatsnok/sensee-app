# Agent Instructions for `shared/tts`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the TTS seam.

## Scope

Applies to:
- `shared/tts/...`

## Local context

`shared/tts` owns the text-to-speech boundary:
- `core` - provider-agnostic contracts such as `Speaker`, `SpeechSynthesizer`, `SpeechHandle`, `VoiceId`, and key verification types.
- `system` and `playback` - platform-backed speech/audio implementations across Android, iOS, JVM, JS, Wasm, and shared `webMain`.
- `elevenlabs` and `openai` - provider adapters.
- `cache` and `database-schema` - persistent audio-clip cache.
- `integration` - routing, provider selection, credentials wiring, cache composition, and Metro providers.

## Local rules

- Keep `core` provider-agnostic and feature-agnostic. Provider SDK/API details belong in provider modules or `integration`.
- Keep platform APIs in the matching source set. Browser speech interop stays in `jsMain`/`wasmJsMain`/`webMain`; Android, Apple, and JVM APIs stay in their platform source sets.
- Treat credentials as secrets. Do not log API keys, raw authorization headers, or full provider request payloads.
- `SpeechHandle.cancel()` must remain meaningful: provider and playback wrappers should stop work and avoid caching partial audio after cancellation or failure.
- Keep provider selection in `integration`; app shell depends on `shared/tts:integration`, not provider modules directly.
- Cache keys must remain deterministic and provider-aware so different engines or quality/model choices do not collide.

## Local verification

- For routing/provider changes, prefer the affected module tests, for example:
  ```shell
  .\gradlew.bat :shared:tts:integration:check
  ```
- For cache behavior, prefer:
  ```shell
  .\gradlew.bat :shared:tts:cache:check
  ```
- For platform source-set changes, compile the narrowest affected target before widening.

## Do not

- Do not introduce provider-specific types into `shared/tts/core`.
- Do not store or print credentials in fixtures, logs, screenshots, test names, or diagnostics.
- Do not cache failed, cancelled, or partial provider streams.
- Do not add a new provider module without routing, key-verification, and cache-key ownership being explicit.

## Related skills

- `.agents/skills/kmp-source-set-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/observability-logging-review`
- `.agents/skills/sqldelight-schema-aggregation-review`
