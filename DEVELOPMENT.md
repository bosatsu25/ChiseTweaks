# ChiseTweaks Development Contract

この文書はChiseTweaksの開発・QA・CI・Release・性能・Versioningに関する**現在仕様の正本**です。ユーザー向け説明は`README.md`、今後の作業計画はGitHub Issues、過去の経緯はGit / Pull Request履歴を参照します。

## 1. Product contract

- Fabric client-only MOD
- Minecraft `26.1.2`
- Fabric Loader `0.19.3` 以上
- Fabric API `0.155.2+26.1.2` 以上
- Java `25` 以上
- runtime featureは現在11個
- 明示仕様がない限り機能を相互排他にしない
- all-features-onを回帰条件として扱う
- custom packet / server installation / remote mod detection / auto downloader / automatic JAR replacementを実装しない
- Analyzerはloaded chunks only。未ロードchunkを強制loadしない

## 2. Toolchain

CIは次を基準にします。

| Component | Contract |
| --- | --- |
| Runner | Ubuntu 24.04 |
| Java | Temurin 25 |
| Python | 3.14 |
| Gradle | 9.7.1 |
| Fabric Loom | 1.17.19 |
| JUnit | 5.14.4 |
| JaCoCo | 0.8.15 |
| PIT Gradle plugin | 1.19.0 |

GitHub Actionsは監査済みfull commit SHAで固定し、Dependabot更新はCIを通して取り込みます。

## 3. Build

通常の品質Gate:

```bash
./gradlew --stacktrace ciGate
```

vanilla placement oracleを含むClient GameTest:

```bash
./gradlew --stacktrace runClientGameTest
```

`ciGate`はJUnit、JaCoCo、PIT、buildを同一Gradle task graphで実行します。

## 4. Versioning

### Authoritative source

`gradle.properties` の `mod_version` を唯一のrepository version sourceとします。

形式:

```text
MAJOR.MINOR.PATCH+mc<Minecraft version>
```

Minecraft互換versionはSemVer build metadataとして扱います。

### Bump policy

- PATCH: bug fix、performance tuning、QA / CI hardening、documentation、UI polish
- MINOR: backward-compatibleなuser-facing capability、新しいfeature / analyzer / workflow
- MAJOR: intentional incompatible config/runtime/API contract、migrationを要する削除、stable 1.0宣言

Versionは手編集ではなく次を使用します。

```bash
python scripts/bump_version.py patch
python scripts/bump_version.py minor
python scripts/bump_version.py major
```

確認だけ行う場合:

```bash
python scripts/bump_version.py minor --dry-run
```

helperは現在の`mod_version`と`minecraft_version`を検証し、`+mc...`を維持した1-step SemVerだけを生成します。malformed / metadata mismatchでは書き込み前にfail closedします。

`build.gradle`は`project.mod_version`をproject versionとして使用し、runtime JAR名と`fabric.mod.json`のversionはこの値から生成されます。

## 5. Release progression guard

CIは最新のofficial SemVer Releaseが存在する場合、そのversionとrepository versionを比較し、PATCH / MINOR / MAJORの正しい1-step incrementでなければ失敗します。

SemVer判定は`scripts/version_policy.py`と`scripts/versioning_core.py`を正とし、同じpolicyをversion helperとCIから利用します。

同一versionを別main commitへ再利用しません。

## 6. CI quality model

`.github/workflows/ci.yml`が品質pipelineの正本です。

Verification layers:

- JUnit: functional contracts、state transition、boundary、UI/config regression
- JaCoCo: retained deterministic scope。line coverage threshold `96%`
- PIT: semantic policy/state-transition scope。mutation score / test strength threshold `96%`
- Client GameTest: Minecraft runtimeでvanilla placement stateをoracleとして比較
- Repository / Source Usage / Documentation / Compatibility / Functional Parity audits
- Artifact / Visual Asset / Release Residue audits
- Prism runtime acceptance: 実GPU、描画、入力、実機組み合わせ

Coverageはblack-box / runtime acceptanceの代替ではありません。

## 7. Test design

回帰設計ではJSTQB Foundation相当の以下を組み合わせます。

- equivalence partitioning
- boundary value analysis
- decision table testing
- state transition testing
- deterministic white-box coverage
- error guessing

Minecraft placement予測では、production側の式をtest側へ複製しません。**実際のvanilla placement結果をoracle**にします。

GUIでは狭幅、日本語/英語、長文、scroll、scissor、footer/button overlapを境界条件として扱います。

Security/configではmalformed UTF-8、unsafe path、symlink、oversized payload、atomic write failureなどをfail-closed条件として扱います。

## 8. Runtime JAR policy

Installable artifactはruntime JARだけです。

- final target: `358400 bytes` 以下（350 KiB）
- frozen / effective hard ceiling: `446814 bytes`
- feature parity / correctnessを壊す削減は禁止
- hard ceilingを機能追加の都合で引き上げない
- SourceFile / LineNumberを維持する
- shrinker / obfuscationを容量目的だけで導入しない

Capacity Recoveryはsafe reductionを優先し、maintainability / testabilityを壊して数値だけを達成しません。

## 9. Official Release

`.github/workflows/release.yml`は、成功した`main` pushのCI runが保持した**exact CI-verified runtime JAR**を取得して公開します。

Release publicationはruntime JARを再build・再pack・version rewriteしません。

公開前後に次を検証します。

- stale main resultではない
- JAR filenameとembedded versionが一致
- Minecraft / Fabric / Java metadata
- client-only metadata
- duplicate / dangling tag拒否
- one-step SemVer
- Release assetはruntime JAR 1個
- published SHA-256がCI artifactと一致
- tag targetがverified main SHAと一致

## 10. Prism acceptance

CIは実GPU / Windows display pathを再現できないため、release acceptanceではPrismで最低限次を確認します。

- clean startup、Mixin errorなし
- 日本語/英語と代表GUI scaleで5タブが使用可能
- Crosshair Inspector / Placement Previewが読める
- unsupported itemでmisleading previewを出さない
- Block FilterがBlockEntity / Bright Chestを正しく抑制
- all-features-onをOverworld / Netherでsmoke
- Lava / Ancient Debris Analyzerに強制chunk loadや長時間停止がない
- disconnect / dimension changeでstale session stateが残らない

ログやevidenceへserver address、username、absolute local path、UUIDを追加しません。

## 11. Performance evidence

実FPS / frametimeはCI wall-clockで代用せず、同一Prism環境の実測値で比較します。

主要metric:

- startup_ms
- p50 / p95 / p99 frametime
- heap MiB
- allocation MiB/s
- render-thread CPU
- average FPS

必要なperformance-sensitive milestoneでJFRを取得します。機能実装ごとに固定本数のJFRを義務化しません。

CIでは決定的contractとして、blocking wait禁止、Analyzer force-load禁止、scan budget / cache上限、artifact sizeなどを監査します。

## 12. Security / privacy

- client-only
- no telemetry / external network
- no packet automation
- no click/key injection
- no world mutation
- no sign/book/chat content capture
- no inventory/container content capture
- no UUID collection
- no block coordinate logging
- prediction / comparison stateは必要最小限、memory-only、bounded

## 13. Repository ownership

Repository内の文章は次の2つへ集約します。

- `README.md`: user-facing current product / install / feature information
- `DEVELOPMENT.md`: developer / QA / CI / release / performance / versioning current contract

RoadmapはGitHub Issues、historyはGit / PR履歴を正とします。日付付き監査メモや過去CI hotfix文書をcurrent仕様として残しません。

Machine-readable / executable contractは文章へ統合しません。`quality/`、distinct audit scripts、Gradle wrapper、workflow、LICENSE / NOTICEはその責務のまま保持します。

Gradle build logicもファイル数だけを理由に巨大統合しません。visual asset generation、JAR compaction、lightweight budgetなど責務が異なるscriptは独立性を維持し、重複が実証された場合だけ統合します。
