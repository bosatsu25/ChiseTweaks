# ChiseTweaks CI Environment

CIとOfficial Releaseで使用する検証済みtoolchainを明示する。

| Component | Contract |
|---|---|
| Runner | Ubuntu 24.04 |
| Java | Temurin 25 |
| Python | 3.14 |
| Gradle | 9.7.1 |
| actions/checkout | v7.0.1 commit SHA固定 |
| actions/setup-java | v5.7.0 commit SHA固定 |
| actions/setup-python | v7.0.0 commit SHA固定 |
| gradle/actions/setup-gradle | v6.3.0 commit SHA固定 |

`verify-build.yml` は実際に起動したJava/Python/Gradleのバージョンを検査し、契約から外れた場合はaggregate quality gateをFAILさせる。

GitHub Actionsは可変major tagではなく、監査済みのfull commit SHAを参照する。バージョン更新時はSHAとこの表を同時に更新する。

Java 25はMinecraft/Fabric側の製品互換契約として固定する。PythonとGradleはCI補助ツールであり、互換性確認後に安定版へ更新する。
