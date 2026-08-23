# Runtime JAR policy

The installable artifact is the runtime JAR `chise-tweaks-<version>.jar`. Sources and QA reports are not Prism mods.

A CI runtime JAR is retained only after the aggregate quality gate succeeds. It is uploaded as the single JAR itself, not as a user-facing ZIP bundle, and is intended for short-lived verification/testing.

An official release JAR must come from the exact `main` SHA that passed CI and must pass artifact metadata, client-only, generated-asset and residue audits before publication. GitHub Release remains the canonical long-lived distribution channel.
