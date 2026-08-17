# ChiseTweaks Performance SQA

This document defines the manual measurement stage that starts only after CI is green. It is intentionally dependency-free: ChiseTweaks must not ship telemetry, background profilers, network checks, or benchmarking libraries in the runtime JAR.

## Goal

Prove that ChiseTweaks remains lightweight in the conditions that matter to players, especially when Ore Highlight and Kelp Highlight are enabled together.

The architecture target is:

- no continuous Ore/Kelp world scan
- no Ore/Kelp raycast loop
- no background polling
- no runtime asset generation
- no network traffic for visual features
- target resolution outside the steady-state render hot path
- runtime JAR below 1,000,000 bytes preferred and always below 1,500,000 bytes

## Fixed environment

Use one Prism instance as the source and clone it for comparisons. Do not change any item below between captures:

- Minecraft 26.1.2
- Java 25
- Fabric Loader 0.19.3
- Fabric API version
- complete mod list except the intentionally compared visual mod
- JVM memory arguments
- world/save
- player coordinates and camera direction
- render/simulation distance
- resolution, graphics options and frame cap
- resource pack order
- shader state and shader pack

Do not publish machine-local paths, usernames, server addresses, access tokens, session identifiers, or full logs without redaction.

## Scenarios

Run every scenario at least three times after a warm-up pass.

1. Baseline: ChiseTweaks absent.
2. Chise idle: ChiseTweaks installed, visual features OFF.
3. Ore Highlight ON.
4. Kelp Highlight ON in a dense natural kelp forest.
5. Ore Highlight + Kelp Highlight ON.
6. Resource-pack reload followed by the same scenes.
7. Iris installed, shaders OFF.
8. Iris shader ON with the selected pack.

For the visual smoke test also verify:

- `minecraft:kelp` and `minecraft:kelp_plant` both highlight.
- seagrass does not highlight.
- highlights do not appear through opaque walls.
- feature OFF/ON is applied without restarting Minecraft.
- the active resource-pack base model remains visible.
- shader ON does not make the overlay disappear, become opaque, or lose normal depth behavior.
- reconnect/rejoin and resource reload do not leave stale highlight state.

## Metrics

Capture the following when available:

- startup time to usable title screen
- P50 frametime
- P95 frametime
- P99 frametime
- average FPS
- JVM heap usage
- allocation rate (MiB/s)
- render-thread CPU
- GC count/pause observations
- resource reload duration
- visible chunk-rebuild spikes

P95/P99 frametime are more important than average FPS because isolated stalls can feel bad even when the average FPS remains high.

For CPU/allocation evidence, use Java 25 JFR externally from the Prism JVM. Do not add an always-on profiler to ChiseTweaks.

## Capture format

Copy `docs/performance-capture.example.json`, enter at least three runs, and keep the file free of personal/local-path data.

Compare two captures with:

```text
python scripts/performance_compare.py baseline.json candidate.json
```

The tool reports medians and relative deltas. It deliberately does not enforce universal percentage thresholds until repeated measurements establish the normal variance of the test machine.

## Release decision

Do not claim that ChiseTweaks is faster than another Modrinth project from architecture alone. A comparative performance claim requires the same machine, instance, world, camera path, settings, warm-up, repetition count, and measured metrics for both mods.

If a proposed optimization makes the code more complex but does not improve the repeated P95/P99, CPU, allocation, heap, startup, or reload measurements, revert the optimization.
