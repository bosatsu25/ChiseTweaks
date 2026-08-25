# Warden Risk Analyzer feasibility — Minecraft 26.1.2

Issue: #126

## Decision

Warden Risk Analyzer is feasible as a **bounded client-observation analyzer**, but not as a client-side mirror of the server's Warden spawn tracker.

The implementation for #127 must expose certainty explicitly and must not invent server-only warning state or an exact future Warden spawn position.

Minecraft 26.1.x is deobfuscated and ChiseTweaks already compiles against Mojang's unobfuscated names. The design below therefore uses the 26.1-style Mojang packages/classes rather than legacy Yarn names.

## Evidence inspected

### ChiseTweaks 0.14.1 baseline

- `FeatureSwitches` is the canonical toggle registry.
- `FeatureManager` owns runtime component registration, ticking, session reset and quarantine.
- `AncientDebrisAnalyzerFeature` already provides the right generic lifecycle pattern for a bounded loaded-chunk analyzer:
  - client chunk load/unload events
  - `getChunkNow` / loaded-chunk-only discovery
  - bounded bootstrap work per tick
  - bounded validation work per tick
  - retained marker snapshots
  - fail-soft rendering through the existing runtime component quarantine model
- `ThroughWallMarkerRenderer`, `ThroughWallPositionSnapshot`, `ThroughWallRenderGuard` and `NearestPositionBuffer` are reusable rendering/result primitives.

The Warden feature must reuse those lifecycle/render ownership ideas without turning Ancient Debris and Warden discovery into one universal scanner.

### Minecraft 26.1.2 / current 26.1 API surface

Relevant Mojang classes/state:

- `net.minecraft.world.level.block.SculkShriekerBlock`
  - `CAN_SUMMON` is the authoritative block-state property that distinguishes summon-capable shriekers.
- `net.minecraft.world.level.block.SculkSensorBlock`
  - loaded sensor positions/state are client-visible world state.
- `net.minecraft.world.entity.monster.warden.Warden`
  - an already-loaded Warden entity is visible through the client level's loaded entity state.
- `net.minecraft.server.level.ServerPlayer`
  - owns `WardenSpawnTracker` server-side and exposes `getWardenSpawnTracker()` on the server player type.
- `net.minecraft.world.entity.monster.warden.WardenSpawnTracker`
  - owns warning level/cooldown/ticks-since-warning and performs server-level warning decisions.
- `net.minecraft.client.renderer.Lightmap`
  - 26.1 lightmap is GPU-backed.
- `net.minecraft.client.renderer.LightmapRenderStateExtractor`
  - extracts the lightmap render state and contains the Darkness-scale calculation path.

26.1's rendering architecture is materially different from the pre-26.1 `LightTexture` path. Chise must not port an old `LightTexture` gamma/darkness mixin by name or behavior.

## Confidence matrix

| Signal / UI field | Classification | Reason / contract |
|---|---|---|
| Loaded Sculk Shrieker position | `AUTHORITATIVE` | It is current client block state in an already-loaded chunk. |
| Loaded Shrieker `CAN_SUMMON=true/false` | `AUTHORITATIVE` | `CAN_SUMMON` is a block-state property available with the loaded block state. |
| Loaded Sculk Sensor position | `AUTHORITATIVE` | It is current client block state in an already-loaded chunk. |
| Sensor → Shrieker direct "will trigger" relation | `PREDICTED` | Position/range alone cannot prove the future server vibration path/event, occlusion/context, cooldown and intervening state. Render only as potential connectivity if implemented. |
| Loaded Warden entity present | `AUTHORITATIVE` for loaded client scope | Chise can inspect entities currently present in client-loaded state. It says nothing about unloaded entities. |
| "No Warden exists anywhere" | `UNAVAILABLE` | Absence from loaded client entity state is not global server knowledge. |
| Current player's server Warden warning level (`x/4`) | `UNAVAILABLE` | The authoritative tracker is server-side (`ServerPlayer` / `WardenSpawnTracker`); no verified 26.1.2 client synchronization contract exposes it as client-owned state. |
| Chise session-observed shrieker activations | `OBSERVED` | Chise may count only events/block-state transitions it actually observes during the current session. This must never be relabeled as the server warning level. |
| Exact next Warden emergence coordinate | `UNAVAILABLE` | The server owns spawn search and final validation. |
| Possible emergence/risk area around dangerous shriekers | `PREDICTED` | A bounded visualization can show where risk is plausible from loaded client terrain, but cannot promise the server's eventual choice. |
| Darkness mob-effect presence on local player | `AUTHORITATIVE` | The local player's synchronized effect state is client-visible. |
| Safe shader-independent suppression of Darkness visual dimming | `UNAVAILABLE` for v1 | 26.1 moved the lightmap to `Lightmap` / `LightmapRenderStateExtractor`; a vanilla-only injection is possible to investigate, but shader replacements are not proven safe. |

## Product scope approved for #127

### Implement

1. **Dangerous Shrieker discovery**
   - scan only loaded chunks in a bounded radius
   - identify `Blocks.SCULK_SHRIEKER`
   - read `SculkShriekerBlock.CAN_SUMMON`
   - keep dangerous and non-summoning counts separate
   - render dangerous shriekers with bounded retained markers

2. **Sculk Sensor context**
   - discover loaded `Blocks.SCULK_SENSOR` and calibrated sensors if useful to the current API
   - expose counts / nearby context
   - do not claim a sensor→shrieker line is guaranteed connectivity
   - v1 may omit link lines entirely to avoid misleading certainty

3. **Existing Warden detection**
   - inspect client-loaded entities for `Warden`
   - expose `Loaded Warden: detected / not detected in loaded scope`
   - never turn the negative case into a global safety claim

4. **Observed warning signal**
   - optional for v1
   - if implemented, count only clearly observed current-session shrieker activations
   - label `Observed`, never `Warning level`
   - reset on disconnect/dimension change

5. **Possible emergence / risk visualization**
   - label as `Possible` / `Predicted`
   - derive only from loaded dangerous-shrieker context and bounded terrain knowledge
   - do not display a single exact future spawn point

6. **Analysis Vision status**
   - expose an explicit unavailable/degraded status for v1 rather than changing gamma or installing a shader-fragile hook
   - actual Darkness effect/state remains untouched
   - no persistent brightness ownership

### Do not implement in #127 v1

- authoritative server warning level
- `x / 4` warning UI
- exact spawn coordinate prediction
- force-loading chunks
- a custom packet or server companion
- persistent gamma writes
- a universal environmental scanner
- a second feature registry
- a second rendering ownership model

## Scan/event model

Use a Warden-specific discovery algorithm behind the existing generic runtime lifecycle.

Recommended bounds for v1:

- default horizontal analysis range: `48 blocks`
- hard maximum horizontal range: `64 blocks`
- loaded chunks only
- maximum tracked chunks derived from the hard range, capped explicitly
- bootstrap chunk lookups split across ticks
- validation split across ticks
- maximum dangerous-shrieker markers: `64`
- maximum sensor results retained for context: `128`
- no per-tick full cubic world scan
- no block-entity NBT reads

Chunk discovery should mirror the safe shape of Ancient Debris Analyzer, but use its own Warden-specific section predicates and result model. Reuse the marker/render/cache primitives where their semantics actually match.

## Darkness / Analysis Vision hook decision

### 26.1 rendering facts

`LightTexture` is no longer the current architecture. 26.1 uses GPU-backed `Lightmap`, with `LightmapRenderStateExtractor` producing `LightmapRenderState`. The extractor has a Darkness-scale calculation path.

### Options evaluated

1. **Persistent gamma override** — rejected.
   - owns a user setting Chise does not own
   - risks stale state after crash/disconnect
   - does not isolate Darkness semantics cleanly

2. **Mixin around Darkness scale in `LightmapRenderStateExtractor`** — technically plausible for vanilla, but rejected for #127 v1.
   - private rendering internals are a fragile injection surface
   - Sodium/Iris/shader-pack replacement/interaction is not yet proven for supported BuilderPack configurations
   - failure here can damage the whole lighting pipeline rather than only Analyzer UX

3. **Capability-degraded UI** — accepted for #127 v1.
   - Analyzer data remains useful
   - `Analysis Vision: unavailable` is honest
   - no lighting corruption risk
   - can be revisited later with real shader matrix evidence

## UI model

Integrate into the existing **Analyzer** surface. Do not add a sixth tab.

Suggested information hierarchy:

```text
Warden Risk Analyzer        [ OFF ]

Dangerous Shriekers          3   AUTHORITATIVE
Other Shriekers              1   AUTHORITATIVE
Nearby Sculk Sensors         5   AUTHORITATIVE
Loaded Warden                DETECTED / NOT DETECTED IN LOADED SCOPE
Server Warning Level         UNAVAILABLE
Possible Risk Area           ON   PREDICTED
Analysis Vision              UNAVAILABLE
```

Do not show unsupported values as zero. `UNAVAILABLE` is semantically different from `0`.

## Reuse / commonization plan

Reuse:

- `FeatureSwitches` for the first-class toggle
- `FeatureManager` runtime registration/tick/session lifecycle
- `FeatureAvailabilityPolicy`
- existing component quarantine/fail-soft behavior
- `ThroughWallMarkerRenderer`
- `ThroughWallPositionSnapshot`
- `ThroughWallRenderGuard`
- `NearestPositionBuffer`
- loaded-chunk event model and bounded bootstrap/validation scheduling pattern
- existing five-tab settings architecture

Keep separate:

- Warden-specific section predicates
- shrieker/sensor classification
- confidence/source model
- Warden entity observation
- possible-risk semantics

No `UniversalScanner`, `EnvironmentalScanner`, or equivalent abstraction is justified.

## Compatibility risks

| Area | Risk | Mitigation |
|---|---|---|
| Sodium | medium | Keep world-marker rendering on Chise's existing Fabric rendering path; no new Sodium hard dependency. |
| Iris, shader disabled | medium | Same as Sodium; runtime analyzer remains independent from shader internals. |
| Shader enabled | high for Analysis Vision, low/medium for markers | Keep Analysis Vision unavailable for v1; validate markers in Prism acceptance rather than modifying lightmap internals. |
| Client/server certainty | high | Confidence labels are part of the data model, not decorative UI text. |
| Chunk scan cost | medium | loaded-only, bounded radius, fixed result caps, split bootstrap/validation work. |
| JAR budget | medium/high | Favor one compact feature/result model and reuse retained rendering primitives; hard ceiling stays unchanged. |

## Size / runtime budget

Implementation target for #127:

- compressed runtime JAR growth target: **<= 24 KiB**
- hard repository ceiling remains `446,814 bytes`
- no change to the hard ceiling
- no unbounded retained collections
- idle OFF path: constant-time switch check / cleared state
- ON scan work: fixed per-tick chunk budget
- entity detection: bounded to client-loaded entity iteration and throttled if profiling shows a need

If the implementation cannot fit the hard JAR ceiling safely, stop and recover capacity separately rather than deleting unrelated behavior.

## Test strategy for #127

### Deterministic JUnit/policy tests

- confidence enum / UI label mapping
- range clamp and chunk relevance
- result caps
- dangerous vs non-summoning shrieker classification
- potential-risk classification never upgrades to authoritative
- server warning level is represented as unavailable
- exact emergence point is never exposed
- session reset model

### Architecture contracts

- first-class feature appears exactly once in `FeatureDefinition` / `FeatureSwitches`
- no packet sending / custom networking
- no gamma setting writes
- no force-load API
- no universal scanner class
- bounded collection constants present

### Client GameTest/runtime

Where the 26.1.2 client test API permits deterministic setup:

- `CAN_SUMMON=true` shrieker detection
- `CAN_SUMMON=false` classification
- loaded Warden entity detection or equivalent runtime contract
- toggle OFF clears retained state
- disconnect/dimension reset
- coexistence with every retained feature enabled

### Prism acceptance

Real Windows/Prism/GPU evidence remains necessary for:

- through-wall marker readability
- Sodium/Iris/shader behavior
- settings layout at supported GUI scales
- log cleanliness

This is not replaceable by JUnit.

## Explicit unknowns / deferred questions

- Whether a supported shader configuration exposes a stable way to suppress only Darkness dimming without replacing shader-owned lighting.
- Whether session-observed shrieker transitions add enough product value to justify their event-tracking complexity in v1.
- Whether sensor→shrieker link visualization is useful enough to justify a predicted relation; v1 should prefer counts/context over potentially misleading lines.
- Exact byte cost remains unknown until CI builds the #127 artifact.

## Final implementation gate

#127 may proceed with the bounded analyzer described above.

The core rule is:

> **Show what the client knows, label what it only predicts, and explicitly mark server-only state unavailable.**
