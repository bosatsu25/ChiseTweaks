# Verified release flow

1. Pull-request CI runs repository/source audit, compile, JUnit, JaCoCo, PIT and artifact audits.
2. The aggregate quality gate is the required branch-protection context.
3. After merge, the same CI runs on `main`.
4. Official Release may publish only the exact `main` commit SHA whose CI run succeeded.
5. Release publication rebuilds and audits the runtime JAR from that exact SHA; normal quality gates remain owned by CI.

This separation avoids duplicate ownership while preserving a clean rebuild and published-artifact verification.
