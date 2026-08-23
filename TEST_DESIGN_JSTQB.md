# ChiseTweaks Test Design — JSTQB Foundation Level Techniques

## Purpose

This document defines the regression and security test conditions for the retained ChiseTweaks scope after the large UI/configuration refactor.

The design applies JSTQB Foundation Level test techniques rather than relying on line coverage alone:

- equivalence partitioning
- boundary value analysis
- decision table testing
- state transition testing
- statement/branch-oriented white-box coverage where deterministic
- error guessing based on prior UI/config/security failure modes

The automated suite is intentionally split by responsibility. Pure policy and geometry are tested headlessly. Minecraft rendering/mixin behavior remains subject to the existing Prism runtime smoke gate because a headless JUnit test must not be presented as proof of in-game rendering correctness.

## Quality risks

| Risk | Impact | Main test type |
|---|---|---|
| Narrow GUI causes overlapping/off-screen controls | Settings become unusable | UI regression / BVA |
| Removed/added navigation sections desynchronize fixed geometry constants | Layout silently degrades after refactor | UI regression / EP |
| Japanese/English settings diverge structurally | Different behavior by language | Unit / metamorphic regression |
| Long Japanese/English text breaks row layout | Text obscures controls | UI regression / EP + BVA |
| Ancient Debris bootstrap scans a full large neighborhood in one tick | Join/movement frame-time spike or temporary FPS loss | Performance contract / BVA |
| Analyzer lookup accidentally force-loads chunks | Client hitching, extra memory/network pressure | Contract / error guessing |
| Lava boundary logic treats an unloaded neighbor as non-lava | False-positive source marker at chunk borders | Decision table / state transition |
| Unicode is truncated inside a surrogate pair | Invalid diagnostic/log text | Security / BVA |
| Invisible/bidirectional characters spoof config keys | Ambiguous or deceptive config input | Security / EP |
| Path traversal or Windows device name escapes the intended config boundary | Unsafe local file access | Security / decision table |
| Symlink target/root redirects config I/O | Read/write outside trusted config directory | Security / decision table |
| Oversized or malformed config consumes unexpected resources or is replacement-decoded | Integrity/availability loss | Security / BVA |
| Atomic write interruption corrupts the previous valid config | Loss of recoverable configuration | Security / state transition |
| Removed feature rows return after scope cleanup | UI/runtime mismatch | Unit / error guessing |

## Test conditions and techniques

### 1. Responsive settings geometry

**Equivalence partitions**

- compact category page
- narrow category page requiring stacked search/bulk controls
- normal/wide category page with inline search/bulk controls
- non-category page without a bulk control
- valid navigation counts (1, current count, larger future count)
- invalid/non-positive navigation count

**Boundary values**

- content width 291 / 292 px around the inline toolbar requirement (`150 + 8 + 134`)
- content width 719 / 720 px around the stacked text breakpoint
- 320×240 Minecraft-style compact GUI
- wide 1920×1080 GUI with the 1180 px content cap

**Regression oracles**

- content, navigation, search, bulk, panel and footer remain inside the screen/content region
- search and bulk never overlap
- Reset / Apply / Done never overlap
- rendered navigation width is derived from the actual navigation count
- panel ends before the footer

Automated by `ChiseTweaksSettingsLayoutTest`.

### 2. Settings row text layout

**Equivalence partitions**

- null/blank text
- text fitting one line
- English text with whitespace break opportunities
- Japanese text without spaces
- supplementary Unicode characters
- text exceeding two lines

**Boundary values**

- max width <= 0
- max width smaller than the ellipsis glyph
- exact one-line fit
- second-line overflow requiring ellipsis

**Regression oracles**

- at most two lines are produced
- each rendered line stays within its width budget
- wrapping/ellipsis never emits malformed UTF-16

Automated by `ChiseTweaksRowTextLayoutTest`.

### 3. Settings structure and language consistency

**Equivalence partitions**

- Resources
- Visibility
- Guide/Help navigation-only page
- Japanese / English presentation

**Metamorphic relation**

Changing display language may change labels/descriptions but must not change row IDs, row kinds, config bindings or action structure.

**Error guessing**

Explicitly reject reintroduction of removed Pumpkin Scaffold, Placement Guide, Sodium lava integration or obsolete lava color rows.

Automated by `ChiseTweaksSettingsControllerTest` plus the existing repository scope contracts.

### 4. Secure config storage

The storage boundary is modeled as a decision table over root state, target state, filename class and payload size.

| Root | Target | Filename | Size | Expected |
|---|---|---|---|---|
| missing | missing | safe leaf | valid | write creates root; read before write is empty |
| directory | missing | safe leaf | valid | allowed |
| directory | regular file | safe leaf | valid | read/replace allowed |
| directory | directory | safe leaf | valid | reject |
| directory | symlink | safe leaf | valid | reject |
| symlink | any | safe leaf | valid | reject |
| directory | any | traversal/absolute/reserved/unsafe leaf | valid | reject |
| directory | regular/missing | safe leaf | max | allow |
| directory | regular/missing | safe leaf | max+1 | reject |

**Boundary values**

- size budget 0 / N-1 / N / N+1
- filename length 128 / 129
- strict UTF-8 valid / malformed

**State transition model for atomic write**

1. existing target is unchanged
2. staged owner-only temp file is created
3. staged content is synchronized and verified
4. root and target are revalidated
5. staged file replaces target
6. replacement postcondition is verified
7. staging file is removed

Fault injection after state 3 must leave the previous regular target untouched and must remove the staged file. A target changed to a non-regular path between states 3 and 4 must fail closed.

Automated by `SecureConfigStorageTest` and `SecurityPrimitiveTest`.

### 5. Runtime Unicode/security policy

**Equivalence partitions**

- ordinary diagnostics
- line/control characters
- `${...}` interpolation-shaped text
- bidirectional/invisible formatting characters
- Unicode noncharacters
- BMP text
- supplementary code points
- NFC / non-NFC filenames and JSON keys
- Windows reserved / ordinary filenames

**Boundary values**

- diagnostic length `MAX-2 + supplementary code point` (exact fit)
- diagnostic length `MAX-1 + supplementary code point` (must not split)
- JSON key and filename length 128 / 129

Automated by `RuntimeSecurityPolicyTest` and `SecurityBoundaryContractTest`.

### 6. Analyzer correctness and frame-time budgets

The Lava Source Highlight and Ancient Debris Analyzer share retained rendering infrastructure, but their discovery policies remain feature-specific. Tests must therefore guard both the shared architecture and the different scan semantics.

**Ancient Debris equivalence partitions**

- non-Nether level: no scan/markers
- Nether with no relevant loaded chunks
- Nether with relevant loaded chunks but no Ancient Debris
- loaded chunk containing Ancient Debris
- tracked chunk unloaded or moved outside the current neighborhood
- range/player-chunk change requiring bootstrap refresh

**Ancient Debris boundary values**

- range 16 / 64 / 256 blocks
- bootstrap radius 2 / 5 / 17 chunks
- bootstrap neighborhood 25 / 121 / 1225 chunk coordinates
- at most 64 scheduled chunk probes per client tick
- max marker count 8 / 64 / 128

The default 64-block bootstrap therefore completes in at most two client ticks when every scheduled coordinate must be checked. The 256-block maximum is intentionally spread across multiple ticks instead of performing all 1225 probes synchronously.

**Lava decision table**

| Enabled | Source lava | Known non-source/non-lava neighbor | Expected marker |
|---|---|---|---|
| no | any | any | no |
| yes | no | any | no |
| yes | yes | no | no |
| yes | yes | yes | yes |

An unloaded neighboring chunk is **unknown**, not evidence of a source boundary. The analyzer must not force-load it merely to complete the boundary test.

**Performance/regression oracles**

- analyzer discovery uses already-loaded client chunks only (`getChunkNow`); no force-load path is introduced
- Ancient Debris full-neighborhood bootstrap is queued and budgeted across ticks
- newly loaded relevant chunks may still be indexed immediately through the chunk-load callback
- Lava remains bounded by horizontal radius <= 8, vertical radius <= 5 and adaptive interval >= 5 ticks
- maximum Lava scan volume at the configured bounds is `(17 * 17 * 11) = 3179` block positions per scan
- chunk unload, dimension/session reset and runtime quarantine clear stale analyzer state
- retained rendering may be shared, but source detection semantics must not be merged

Automated by `AncientDebrisAnalyzerPolicyTest`, `SettingsAndAnalyzerDesignContractTest` and the existing analyzer policy tests. Actual FPS/frametime and wall-through rendering remain Prism runtime-smoke responsibilities.

## Defects cleaned during this test pass

1. **Stale navigation cardinality** — layout geometry still assumed four navigation buttons after the retained UI was reduced to three sections. Runtime geometry now receives the actual section count.
2. **Compact footer overlap** — fixed Reset/Apply/Done widths exceeded compact content width and could overlap. Footer button geometry now compresses without overlap.
3. **Sub-360 px overflow** — geometry previously substituted a synthetic minimum width/height, allowing controls to extend beyond the real GUI. Layout now uses the real viewport dimensions and stacks the toolbar when required.
4. **Search accessibility label language mismatch** — the EditBox narration label was always Japanese even in English UI. It now follows the active language.
5. **Diagnostic surrogate splitting** — character-count truncation could cut a supplementary Unicode code point in half. Sanitization is now code-point aware.
6. **Whitespace-spoofed JSON keys** — JSON object keys with leading/trailing whitespace are now rejected by the security policy.
7. **Single-tick Ancient Debris bootstrap burst** — range/chunk changes could synchronously inspect the entire loaded-neighborhood candidate set in one tick (up to 1225 coordinates at maximum range). Bootstrap lookup/scanning is now queued and capped at 64 coordinates per tick.

## CI acceptance criteria

The branch is acceptable only when all of the following are true:

1. all JUnit unit/contract/UI-regression/security tests pass;
2. existing retained-scope JaCoCo line coverage gate remains >= 96%;
3. existing retained-scope PIT mutation score remains >= 96%;
4. existing retained-scope PIT test-strength gate remains >= 96%;
5. repository audit passes;
6. build succeeds on Java 25;
7. manual Prism runtime smoke testing remains required before a release.

Coverage is treated as a supporting white-box measure, not a substitute for the black-box conditions above.
