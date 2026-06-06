# Chapter 7 — Deployment View (Представление развёртывания)

## Purpose
Describe the technical infrastructure the system runs on (environments, devices, processors, network topology) and the mapping of building blocks onto that infrastructure. Important for distributed or multi-environment systems.

## Required / recommended subsections
- **Infrastructure Level 1**: overview diagram, motivation, quality/performance characteristics, and the mapping of building blocks to infrastructure.
- **Infrastructure Level 2+**: internal structure of selected infrastructure elements.
- Copy/adapt per environment (dev/test/prod) when they differ materially.

## Form
UML deployment diagram(s) combined with tables/text.

## Do not put here
- Logical decomposition (→ §5). Runtime behavior (→ §6).

## Related chapters
§5 (blocks being deployed), §6 (Runtime), §2 (technical constraints). In Sensee, platform deployable targets live in the LikeC4 `deployment {}` model.

## Source
arc42 §7 — https://docs.arc42.org/section-7/
