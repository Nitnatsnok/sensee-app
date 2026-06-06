# Chapter 3 — Context and Scope (Контекст и рамки)

## Purpose
Delimit the system from its communication partners (neighboring systems and users): define external interfaces, separating business context (domain inputs/outputs) from technical context (channels, protocols, hardware).

## Required / recommended subsections
- **Business Context** — all communication partners with their domain-specific inputs/outputs. A context diagram (system as a black box) and/or a table (partner · input · output).
- **Technical Context** — technical interfaces (channels, transmission media, protocols); a mapping of domain inputs/outputs to channels where it is not obvious.

## Form
Context (black-box) diagram plus a table; keep external interfaces explicit.

## Do not put here
- Internal decomposition (→ §5). Deployment infrastructure (→ §7).

## Related chapters
§5 (Building Block View zooms inside the black box), §7 (Deployment).

## Source
arc42 §3 — https://docs.arc42.org/section-3/
