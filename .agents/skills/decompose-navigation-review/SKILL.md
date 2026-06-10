---
name: decompose-navigation-review
description: Use this skill when Sensee work touches Decompose components, ScreenConfig or NodeConfig types, child stacks, child panels, section hosts, deep links, route serialization, navigation-api modules, result dispatching, or back handling.
---

# Decompose Navigation Review

Status: active.

## Operating mode

Review/support. Map ownership and risks before proposing navigation changes.

## Non-goals

- Do not move business logic into navigation configs.
- Do not extract `navigation-api` modules unless another module actually needs the contract.

## Workflow

1. Read affected component contracts, implementation, navigation configs, and closest `AGENTS.md`.
2. Map which module owns each config and component boundary.
3. Check restoration, serialization, result dispatch, and back handling.
4. Verify docs/LikeC4 impact for architectural navigation changes.

## Check

- Persisted root or section-stack configs are `@Serializable` and registered through a `ScreenConfigSerializersProvider` aggregated under `shared/core/decompose` (ADR 0003).
- Feature APIs do not depend on other feature implementations.
- `navigation-api` is separated only when external modules need entry contracts.
- Root, section, host, and leaf responsibilities are not mixed.
- State restoration remains valid.
- Back handling is correct; treat predictive-back gesture/animation as out of scope unless the change explicitly adds it.
- Child stack updates happen through established Decompose/navigation abstractions.
- URL/deep-link projection does not duplicate business navigation logic.

## Verification

- Module-scoped checks:
  ```shell
  .\gradlew.bat :shared:core:decompose:check
  .\gradlew.bat :shared:app-shell:check
  ```

## Output

- Navigation flow map
- Ownership of each config
- Serialization risks
- Back stack/state restoration risks
- LikeC4/docs impact
- Minimal change recommendation

## Related skills

Components are state holders: use `compose-state-holder-ui-split` (state-holder/UI split) and `kotlin-flow-state-event-modeling` (component state and one-shot events) when available in the running agent.
