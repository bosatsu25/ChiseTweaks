## Change summary

Describe the user-visible or engineering change in a few sentences.

## Product risk / test basis

- Risk register IDs:
- Quality characteristic(s):
- Test technique(s):
- Oracle:
- Exit criteria affected:

If no existing risk applies, explain whether `quality/risk-register.json` should be updated.

## Scope checklist

- [ ] The 16-feature runtime product contract is preserved, or the intentional change is documented.
- [ ] No ChiseTweaks-owned automation, click/key injection, custom play protocol, background execution, or server requirement is introduced.
- [ ] Through-wall analyzers remain bounded, loaded-chunk-only, and do not bypass server-side obfuscation.
- [ ] Optional integrations remain fail-soft when their target mod is absent or its API drifts.
- [ ] Session / disconnect / dimension cleanup was considered.
- [ ] Config schema or migration impact was considered.
- [ ] Hot paths were reviewed for new allocation, registry lookup, String conversion, blocking work, or duplicate world traversal.
- [ ] Runtime JAR size impact was considered against the 358,400-byte target and 446,814-byte hard ceiling.
- [ ] Prism / JFR / manual acceptance evidence is identified when deterministic CI cannot provide the oracle.

## Verification

List the exact automated tests, Client GameTests, audits, and manual evidence used.

## Defect prevention / RCA

For bug fixes or escaped regressions, complete this section. For behavior-preserving refactors, write `N/A - refactor`.

- Root cause:
- Escape point — why the existing gate did not catch it earlier:
- Similar-risk search — where else the same defect pattern was checked:
- Regression test:
- Preventive action / contract:

## Refactoring safety

- [ ] The change reduces time-to-understanding or coupling rather than splitting files only by size.
- [ ] Source-contract assertions are limited to architecture/security/performance constraints; behavior is verified behaviorally where practical.
- [ ] No unrelated production responsibility was moved into a lower-level layer.
