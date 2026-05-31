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

- Persisted root or section-stack configs are serializable and registered through the common serializer assembly under `shared/core/decompose`.
- Feature APIs do not depend on other feature implementations.
- `navigation-api` is separated only when external modules need entry contracts.
- Root, section, host, and leaf responsibilities are not mixed.
- State restoration remains valid.
- Back handling and predictive-back risks are considered.
- Child stack updates happen through established Decompose/navigation abstractions.
- URL/deep-link projection does not duplicate business navigation logic.

## Output

- Navigation flow map
- Ownership of each config
- Serialization risks
- Back stack/state restoration risks
- LikeC4/docs impact
- Minimal change recommendation
