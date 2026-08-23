# ChiseTweaks CI Environment

CIは再現性のある安定版toolchainで実行し、Official ReleaseはCIで検証したruntime JARを再ビルドせずそのまま公開する。

| Component | Contract |
|---|---|
| Runner | Ubuntu 24.04 |
| Java | Temurin 25（最新25.x patch） |
| Python | 3.14（最新3.14.x patch） |
| Gradle | 9.7.1 |
| Fabric Loom | 1.17.19 |
| JUnit | 5.14.4 |
| JaCoCo | 0.8.15 |
| PIT Gradle plugin | 1.19.0 |
| actions/checkout | v7.0.1 commit SHA固定 |
| actions/setup-java | v5.7.0 commit SHA固定 |
| actions/setup-python | v7.0.0 commit SHA固定 |
| gradle/actions/setup-gradle | v6.3.0 commit SHA固定 |
| actions/upload-artifact | v7.0.1 commit SHA固定 |
| actions/download-artifact | v8.0.1 commit SHA固定 |

## CI design

`.github/workflows/ci.yml` が唯一の品質パイプラインで、repository contract auditの後に `./gradlew --stacktrace ciGate` を1回だけ実行する。`ciGate` はJUnit、JaCoCo、PIT、buildを同一Gradle task graphで実行するため、同じプロジェクトを複数Gradle processで繰り返し構成しない。

CI成功後はruntime JARだけをGitHub Actions artifactとして保持する。通常CIは `contents: read` のみで、tagやGitHub Releaseを作成しない。

## Release design

`.github/workflows/release.yml` は成功したmain pushのCI runを受け取り、そのrunが保持したruntime JARを `actions/download-artifact` で取得して公開する。Java/Python/Gradleのsetup、再ビルド、JUnit/JaCoCo/PIT、artifact再生成は行わない。

これにより、**検証した成果物と公開する成果物が同一byte列**になる。Release側ではstale main、client-only metadata、単一JAR、SHA-256、重複tag/releaseだけを検証する。

GitHub Actionsは可変major tagではなくfull commit SHAを参照する。更新はDependabotでまとめて受け、CIで検証してから取り込む。

Java 25はMinecraft/Fabric側の製品互換契約として固定する。Ubuntu 26.04などpreview runnerやJava 26へは「新しい」という理由だけでは移行せず、製品互換性が取れた時点で更新する。
