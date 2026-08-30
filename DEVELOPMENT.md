# ChiseTweaks Development Contract

この文書はChiseTweaksの開発・QA・CI・Release・性能・Versioningに関する**現在仕様の正本**です。ユーザー向け説明は`README.md`、今後の作業計画はGitHub Issues、過去の経緯はGit / Pull Request履歴を参照します。

## 1. Product contract

- Fabric client-only MOD
- Minecraft `26.1.2`
- Fabric Loader `0.19.3` 以上
- Fabric API `0.155.2+26.1.2` 以上
- Java `25` 以上
- toggle可能なruntime visual featureは現在12個
- Masa ecosystem integrationはruntime visual feature数へ含めず、optional compatibility / UX capabilityとして別registryで管理する
- ChiseTweaks自身はAutomationを実装しない。外部MODが所有する操作にGuard / policy / refreshを追加することだけをIntegrationとして許可する
- 12機能はすべてrendering / inspection-orientedで、building-action featureは持たない
- Bright Chest / Bright Concreteはbuilt-in Resource Pack selection / reloadへ依存しない
- Low FireはMinecraftが`ScreenEffectRenderer.renderFire`へ渡す現在のspriteを再利用し、Large / Medium / Smallの3段階geometryだけを一人称overlayへ適用する。通常炎・魂の炎ごとのChise専用PNG/model、world-fire置換、Resource Pack reloadを持たない
- Bright ChestはChiseTweaks内蔵のChest専用`normal.png` / `normal_left.png` / `normal_right.png`をvanilla CHEST atlas経路で選択し、Chest model・金具・蓋・double-chest分割・開閉animationを維持する
- Bright ChestはWhite Concrete spriteをChestへ流用しない。White Concreteの描画責務はBright Concreteだけが持つ
- Bright Concreteはvanilla White Concrete model / textureを維持し、quad lightingだけをfull-bright化する
- 明示仕様がない限り機能を相互排他にしない
- all-features-on（12機能）を回帰条件として扱う
- custom packet / server installation / remote mod detection / auto downloader / automatic JAR replacementを実装しない
- Lava Analyzerはloaded chunks only。未ロードchunkを強制loadしない
- サーバー側ゲーム進行を変える配置補助や、隠れ資源・server-only状態を探索／推測するAnalyzerは現行スコープ外

### Masa integration contract

- MaLiLib / Litematica / Tweakeroo / TweakerMore / Syncmaticaはhard dependencyにしない
- 対象MODが存在しない場合、対応integrationはno-opかつChise起動を妨げない
- integration設定は`chisetweaks-integrations.json`へ分離し、FeatureDefinitionの12機能と混在させない
- 外部AutomationをChise自身が開始しない
- Syncmatica等の外部packet ownershipをChiseへ移さない

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

vanilla placement oracleとclient runtime regressionを含むClient GameTest:

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
- MINOR: backward-compatibleなuser-facing capability、新しいfeature / analyzer / workflow、または明示的な製品スコープ整理
- MAJOR: intentional incompatible config/runtime/API contract、migrationを要する削除、stable 1.0宣言

Versionは手編集ではなく通常は次を使用します。

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

`.github/workflows/ci.yml`が品質pipelineの正本です。Required check名は `verify / Java 25 quality gate` を維持します。

CI v3は**品質ゲートを削らず、同一treeの重複FULL検証を避ける**構成です。

### Pull Request

- `scripts/ci_scope.py` が変更ファイルをfail-closedで `docs-only` / `tooling-only` / `full` に分類する
- `README.md` / `DEVELOPMENT.md` / `docs/**` だけは `docs-only`
- docsに加えて `.github/**` / `scripts/**` / `quality/**` だけなら `tooling-only`
- source / test / runtime resource / Gradle build logic / config / mixed change / 空集合は `full`
- `docs-only` / `tooling-only` でもPython tooling test、Version progression、Repository / Source Usage / Documentation / Compatibility / Functional Parity auditsは実行する
- `full` はJava / Gradle / JUnit / JaCoCo / PIT / Client GameTest / Artifact / Visual Asset / Release Residue auditsをすべて実行する
- 成功したPR CIは、`scripts/ci_provenance.py`を正本として、実際にcheckoutして検証したGit tree SHA、scope、runtime JAR名 / SHA-256を `chise-ci-provenance` artifactへ保存する
- runtime artifactはFULL distribution audit完了後だけprovenanceへ記録する

### main push

- squash / merge後のmain commitから関連PRを特定し、成功済みPR CIのprovenanceを取得する
- **mainのGit tree SHAとPRで実際に検証したtree SHAが完全一致する場合だけ**PR結果を再利用できる
- tree-identicalな `full` PRでは、PRで検証済みruntime JARをbyte-for-byte再利用し、SHA-256・runtime metadata・JAR size・retired residueをmain側で再監査する。PIT / Client GameTestは重複実行しない
- tree-identicalな `docs-only` / `tooling-only` PRではmainでもheavy runtime gateを実行しない
- tree不一致、provenance欠落、artifact欠落、direct push、判定不能は**必ずFULLへfail closed**する
- `workflow_dispatch` もFULL
- main CIがruntime JARを持たない場合、Release job自体を起動しない
- Release対象runtimeはmain CI runが保持したartifactだけ。PRから再利用する場合もmainでtree / SHA-256を再検証して同一byte列を再uploadする

### Actions budget guard

- Draft PRではjobを起動せず、`ready_for_review`で検証を開始する
- concurrencyは同一refの古いrunをcancelする
- CI timeoutは15分、Release timeoutは5分、CI artifact retentionは3日
- `CHISE_CI_RUNS_ON` Repository Variableが未設定なら `ubuntu-24.04`。必要時はJSON形式のLinux self-hosted runner labelsへ切替可能
- GitHub-hosted runnerのminute / spending limit / payment method自体はGitHub account側設定でありrepository codeから変更しない

FULL verification layers:

- JUnit: functional contracts、state transition、boundary、UI/config regression
- JaCoCo: retained deterministic scope。line coverage threshold `96%`
- PIT: semantic policy/state-transition scope。mutation score / test strength threshold `96%`
- Client GameTest: Minecraft runtimeでvanilla placement stateをoracleとして比較し、全12機能同時ONを検証
- Repository / Source Usage / Documentation / Compatibility / Functional Parity audits
- Artifact / Visual Asset / Release Residue audits
- Prism runtime acceptance: 実GPU、描画、入力、実機組み合わせ

Coverageはblack-box / runtime acceptanceの代替ではありません。CIの分数削減を理由に、未検証treeを検証済みとして扱いません。

## 7. Test design

回帰設計ではJSTQB Foundation相当の以下を組み合わせます。

- equivalence partitioning
- boundary value analysis
- decision table testing
- state transition testing
- deterministic white-box coverage
- error guessing

Minecraft placement予測では、production側の式をtest側へ複製しません。**実際のvanilla placement結果をoracle**にします。Placement Preview / Actual Comparisonは読み取り・比較capabilityであり、実際の入力や配置操作は変更しません。

Bright Chestではsingle / double-left / double-rightの3専用textureが存在し、`ChestRenderer`が`CHEST_MAPPER`経由で各`ChestType`へ正しいspriteを選び、White Concrete spriteへ退行しないことをsource contractで検証します。最終的なtexture atlas / geometry / animationの見え方はPrism実機acceptanceで確認します。

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

`.github/workflows/ci.yml`内の`release / Publish verified runtime JAR` jobは、成功した`main` verify jobがruntime artifactを生成・昇格した場合だけ起動し、**exact CI-verified runtime JAR**を公開します。runtime artifactがないdocs/tooling-only main更新ではRelease runnerを起動しません。

Release publicationはruntime JARを再build・再pack・version rewriteしません。Only one-step PATCH, MINOR, or MAJOR incrementを許可し、検証済みartifactと公開artifactを同一byte列に保ちます。

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
- 日本語/英語と代表GUI scaleで6タブが使用可能
- Crosshair Inspector / Placement Previewが読める
- unsupported itemでmisleading previewを出さない
- Block FilterがBlockEntity / Bright Chestを正しく抑制
- Bright Chestがsingle / double chestともチェスト形状・金具・蓋・開閉animationを維持した白いChestとして描画され、White Concrete面へ退行しない
- Bright Chest / Bright Concrete（White Concrete）が独立して切り替わる
- Low FireのLarge / Medium / Smallが一人称overlayだけへ反映され、通常炎／魂の炎の現在spriteとworld fireを壊さない
- all-features-on（12機能）をOverworld / Netherでsmoke
- Lava Analyzerに強制chunk loadや長時間停止がない
- disconnect / dimension changeでstale session stateが残らない

ログ監査には次を使用します。

```bash
python scripts/prism_acceptance_audit.py <instance-root>/logs/latest.log
```

ログやevidenceへserver address、username、absolute local path、UUIDを追加しません。

## 11. Performance evidence

実FPS / frametimeはCI wall-clockで代用せず、同一Prism環境の実測値で比較します。

代表scenario:

- `chise-absent`
- `chise-all-off`
- `lava-analyzer-on`
- `highlights-on`
- `maximum-supported-load`

主要metric:

- startup_ms
- p50 / p95 / p99 frametime
- heap MiB
- allocation MiB/s
- render-thread CPU
- average FPS

比較にはGradleの`comparePerformanceEvidence`を使います。必要なperformance-sensitive milestoneでJFRを取得しますが、機能実装ごとに固定本数のJFRを義務化しません。

CIでは決定的contractとして、blocking wait禁止、Analyzer force-load禁止、scan budget / cache上限、artifact sizeなどを監査します。

Bright Chestは既存Chest draw pathでspriteだけを切り替え、追加world scan・追加draw call・Resource Pack reloadを持ちません。3枚の専用textureによるJAR増加も既存hard ceiling内で管理します。

## 12. Security / privacy

- client-only
- no telemetry / external network
- no custom play packet / packet automation
- no automatic click/key injection
- no autonomous world mutation
- no hidden-resource scanner
- no server-only threat-state inference
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
