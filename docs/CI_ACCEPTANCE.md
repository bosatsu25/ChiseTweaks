# CI hotfix acceptance

The hotfix is mergeable only when all of these are green on the same pull-request head:

- version policy
- repository/source audit
- Java 25 compile
- JUnit
- JaCoCo >= 96%
- PIT mutation/test strength >= 96%
- artifact/visual/residue audits
- aggregate quality gate

No threshold reduction is allowed to make this hotfix pass.
