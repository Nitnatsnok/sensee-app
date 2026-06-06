# Chapter 5 — Building Block View (Представление строительных блоков)

## Purpose
Mandatory static decomposition of the system into building blocks (modules, components, subsystems, layers…) and their dependencies — the "floor plan". Communicate structure through abstractions while hiding implementation detail.

## Required / recommended structure (hierarchical black/white boxes)
- **Level 1 — whole system as a white box**: an overview diagram, the **motivation** for the decomposition, and **black-box descriptions** of the contained building blocks. Optionally describe important interfaces.
- **Black-box template** per building block: purpose/responsibility; interface(s); (optional) quality/performance characteristics; (optional) directory/location; (optional) fulfilled requirements; (optional) open issues/risks. A table (name · responsibility) is an acceptable compact form.
- **Level 2** — white-box of *selected* Level-1 blocks (their internal black boxes).
- **Level 3+** — zoom further only where it adds value.

## Form
Hierarchical diagrams plus black/white-box descriptions or a responsibility table. Prefer **relevance over completeness**: detail important, surprising, risky, or volatile blocks; skip standard/boring ones.

## Do not put here
- Runtime interaction between blocks (→ §6). Deployment mapping (→ §7). Decision rationale (→ §4/§9).

## Related chapters
§3 (Context), §6 (Runtime uses these blocks), §7 (Deployment maps them), §4/§9 (decisions). In Sensee, structure is also held in the LikeC4 model (`docs/c4/`) and linked, not restated.

## Source
arc42 §5 — https://docs.arc42.org/section-5/
