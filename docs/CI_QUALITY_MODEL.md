# ChiseTweaks CI quality model

This document defines one owner for each quality responsibility so CI and release publication do not drift into duplicate gates.

## Verification layers

- **JUnit**: functional contracts, state transitions, boundary values, UI structure, configuration migration.
- **JaCoCo >= 96%**: broad deterministic retained scope, including UI geometry and UI availability policy.
- **PIT >= 96%**: semantic policy/state-transition code where a mutation represents a meaningful behavior change. Coordinate/layout arithmetic is intentionally verified by boundary-value tests plus JaCoCo instead of being allowed to dilute the mutation score with equivalent geometry mutations.
- **Artifact audits**: runtime JAR identity, generated visual assets, release residue, client-only metadata.
- **Prism runtime acceptance**: actual Minecraft/Fabric rendering, input flow, GPU/frametime behavior.

## Publication rule

A GitHub Release may only be produced from the exact `main` commit SHA that completed the normal CI quality gate successfully. Publication must not weaken or bypass the normal CI result.

## Toolchain contract

- Ubuntu 24.04
- Java 25
- Python 3.14
- Gradle 9.7.1 with wrapper distribution checksum verification
- GitHub Actions pinned to audited full commit SHAs
