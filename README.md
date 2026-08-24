# ChiseTweaks

> **大規模建築・技術施設の「見落とし」を減らす、Fabric クライアント専用 MOD。**

ChiseTweaks は、鉱石・細線・ガラス・危険物などの**視認性向上**、不要なブロックやエンティティを隠す**Visual Filter**、溶岩源や古代の残骸を確認する**Analyzer**を1つにまとめた Minecraft MOD です。

- ✅ **クライアントだけで動作** — サーバーへの導入不要
- ✅ **11個の主要機能を個別にON/OFF** — Highlight / Visual Filter / Analyzer / Visibility
- ✅ **全機能の同時利用を前提** — Fine Line / Hidden Block / Nether Highlight も同時ON可能
- ✅ **loaded chunks only** — 未ロードチャンクを強制ロードしない
- ✅ **Prism Launcherの標準ログで診断** — 独自診断画面を増やさない
- ✅ **自動ダウンロード・独自通信なし** — 自動MOD更新、JAR置換、custom packetを行わない

> [!IMPORTANT]
> 配布JARは Minecraft `26.1.2` 専用です。異なるMinecraft / Fabric / Javaバージョンへ流用しないでください。

## まず使う

### 1. 必要環境

| 項目 | 対応 |
| --- | --- |
| ChiseTweaks | `0.9.4+mc26.1.2` |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 動作側 | Client only |
| サーバー導入 | 不要 |
| Mod Menu | 任意 |
| Sodium | 任意・推奨 |

### 2. 導入

Prism Launcherで Minecraft `26.1.2` / Fabric / Java `25` のインスタンスを用意し、Fabric APIを導入したうえで、Modsへ次のJARを**1個だけ**追加します。

```text
chise-tweaks-0.9.4+mc26.1.2.jar
```

ChiseTweaksはクライアント専用です。サーバーの `mods` フォルダへ入れる必要はありません。

### 3. 設定を開く

Mod Menuを導入している場合は、Mod Menu → **ChiseTweaks** から設定画面を開けます。

主要な11個のruntime featureは**初期OFF**です。`Bright Chest` と `Bright Concrete` はMinecraft標準Resource Packsとして**初期ON**です。

設定ファイル:

- `config/chisetweaks.json`
- `config/chisetweaks-visual.json`

## 機能一覧

設定画面は、役割が分かる4系統だけに整理しています。

| 系統 | 機能 | できること | 初期状態 |
| --- | --- | --- | --- |
| **Highlight** | Ore Highlights | 鉱石・古代の残骸・黒曜石系を強調 | OFF |
|  | Nether Highlight | ネザー建築素材を補助表示 | OFF |
|  | Fine Line Highlight | Tripwire / Tripwire Hookなど細い技術ブロックを強調 | OFF |
|  | Hidden Block Highlight | Powder Snow / Blue Ice / Dead Coral / Sculk Catalystなどを強調 | OFF |
|  | Glass Highlight | Glass / Glass Paneを形状別に強調 | OFF |
|  | Kelp Highlight | Kelp / Kelp Plantへ視認性オーバーレイを追加 | OFF |
| **Visual Filter** | Block Filter | Block IDのAllow / Hide listでローカル描画を制御 | OFF |
|  | Entity Filter | Entity IDのAllow / Hide listでローカル描画を制御 | OFF |
| **Analyzer** | Lava Analyzer | 読み込み済み範囲の**溶岩源**を輪郭＋半透明面で表示 | OFF |
|  | Ancient Debris Analyzer | Netherの読み込み済みチャンクから古代の残骸を検出 | OFF |
| **Visibility** | Low Fire | 一人称の炎オーバーレイを低く表示 | OFF |
|  | Bright Chest | チェストを見つけやすくするbuilt-in Resource Pack | ON |
|  | Bright Concrete | White Concreteを見つけやすくするbuilt-in Resource Pack | ON |

### Highlight

**Ore Highlights** は鉱石family、個別target、animation、modded ore互換設定に対応します。

Fine Line / Hidden Block / Nether Highlightは**同時にON**にできます。旧排他モードや連動OFF処理は使用しません。

共通Highlight設定は、負荷が無制限に増えないよう次の範囲へ制限されています。

| 設定 | 範囲 |
| --- | ---: |
| 水平範囲 | `1–8` |
| 垂直範囲 | `1–5` |
| 更新間隔 | `5–100 ticks` |
| 最大オーバーレイ | `1–24` |

### Visual Filter

- **Block Filter** — Block IDのAllow / Hide listでローカル描画を制御
- **Entity Filter** — Entity IDのAllow / Hide listでローカル描画を制御。プレイヤー自身は保護

Visual Filterが変更するのは**クライアント描画だけ**です。サーバー側のblock / entity状態は変更しません。

### Analyzer

#### Lava Analyzer

読み込み済みチャンクの近距離だけを走査し、**溶岩源だけ**を表示します。Flowing Lavaは対象外です。

#### Ancient Debris Analyzer

Nether内の**読み込み済みクライアントチャンクだけ**を対象に古代の残骸を検出します。

| 項目 | 上限・範囲 |
| --- | ---: |
| 検出範囲 | `16–256 blocks` |
| 最大表示数 | `8–128` |
| 初回探索 | 最大 `64 chunks / tick` |
| 再検証 | 最大 `16 chunks / tick` |
| 最大追跡数 | `4096 chunks` |
| 1チャンクあたり最大保持数 | `256` |

初回探索は複数tickへ分散します。追跡済みチャンクも定期再検証するため、後から追加・削除された古代の残骸を反映します。disconnect / reconnect / dimension変更時にはsession cacheを破棄します。

### Visibility

**Low Fire** は一人称の炎オーバーレイだけを下げます。ワールド上の炎モデルやサーバー状態は変更しません。

`Bright Chest` と `Bright Concrete` は、それぞれ独立したbuilt-in Resource Packです。

| Pack | 対象 |
| --- | --- |
| `chisetweaks:chise_chest_visibility` | Chest / Double Chest Left / Double Chest Right |
| `chisetweaks:chise_white_concrete_visibility` | White Concrete |

2つのpackは同一のserialized reload queueを共有し、Minecraftの**delayed texture reload**経路で軽量に反映します。高速に切り替えても重複reloadを避け、最後の要求状態へ収束します。失敗時は既知の正常選択へrollbackします。

## 0.9.4 の主な更新

`0.9.4+mc26.1.2` は、0.9.3で固めたmigration / reload / diagnostics基盤を、Prism実機受入と性能回帰の切り分けまで扱える形へ拡張したリリースです。

<details>
<summary><strong>変更内容を表示</strong></summary>

- Structured diagnosticsをMinecraft標準logへ出力し、Prism Launcher / `logs/latest.log` を診断の正とする
- 設定画面から重複していたResource Reload状態表示・diagnostic copy/export操作を除去
- `Connection reset` をChiseTweaks内部failureと混同しないPrism log auditを追加
- `0.7.7 / 0.9.1 / 0.9.2 / future schema` のconfig fixtureを固定し、migration / downgrade safetyを継続検証
- Resource Reload Coordinatorへ長時間状態遷移stress regressionを追加
- Ancient Debris Analyzerへ長時間budget / retained-capacity regressionを追加
- Sodium / Iris / ImmediatelyFast / EntityCullingについてhard dependency・conflict・implementation namespace直結をCIで禁止
- resource reloadのblocking wait、Analyzerのforce chunk load、budget逸脱をruntime performance contract auditで検出
- 5つのPrism性能scenario用baseline/candidate CSV template generatorを追加
- 検証済み0.9.4 JAR `446814 bytes` をM0 size baselineとして固定し、軽量化中の容量増加を拒否
- runtime JARの最終目標を `358400 bytes`（350 KiB）以下へ固定。ただしFunctional Parityを優先し、機能削除による達成は認めない
- Bright Chest / Bright Concreteの切替をforeground full-resource reloadからMinecraftのdelayed texture reload経路へ変更

</details>

---

# 詳細情報

ここから下は、トラブルシューティング・移行・品質保証・開発向けの情報です。**通常利用だけなら上の「まず使う」「機能一覧」までで十分です。**

## 設定の保存と互換性

設定画面では変更をメモリ上で扱い、**Apply / Done** を永続化境界とします。

設定JSONは検証・サニタイズされ、未知の古いfieldはruntime状態へ直接反映しません。Visual Target schemaは既知のversionから移行し、future schemaを読み込んだ場合も現行target maskへ安全に制限します。

Bright Chest / Bright Concreteの状態はMinecraft標準Resource Packsを正とし、ChiseTweaks JSONへ重複保存しません。

### 0.9.1以前からのVisibility pack移行

旧 `chisetweaks:chise_texture` を使用していたインスタンスでは、初回起動時に現在のChest / White Concrete packへ一度だけ移行します。

migrationは次を区別します。

- 旧packが有効だった既存インスタンス
- 旧packを無効化していた既存インスタンス
- 0.9.2以降ですでに2packを個別変更済みのインスタンス
- 新規インストール
- migration済みインスタンス

無関係なResource Packの順序や選択状態は変更しません。migration完了markerにより同じ移行を繰り返しません。

## Structured Diagnostics

ChiseTweaksは異常時の原因切り分け用に、必要最小限のruntime状態をMinecraft標準logへ**単一行**で出力します。

Prism Launcherでは次のどちらかで確認できます。

1. Prism Launcherのコンソール
2. インスタンスの `logs/latest.log`

主なfield:

- `event`
- `version`
- `sessionId`
- `phase`
- `dimension`
- `enabled`
- `visibilityPacks`
- `quarantined`
- `reloadState`
- event固有の `componentId` / `failure` / `source` / `reason` など

`reloadState` は `idle / reloading / recovery_pending / reloading_with_recovery` のいずれかです。診断状態を設定画面へ重複表示せず、`Chise diagnostics event=...` のlog行を診断の正とします。

対象eventにはstartup / join / disconnect、component quarantine、resource selection/reload/recovery、Visibility pack migrationがあります。

> [!NOTE]
> サーバーアドレス、ユーザー名、ローカル絶対pathなどはdiagnostic snapshotへ収集しません。detailも長さと文字種を制限し、改行や制御文字をそのままlogへ入れません。

## トラブルシューティング

### まず確認するもの

Minecraftプロセスが終了した、画面が固まった、描画例外が発生した場合は、次の順に確認してください。

1. Prism Launcherのコンソール / `logs/latest.log`
2. `crash-reports/crash-*.txt` が存在する場合はそのファイル
3. `disconnect-*-client.txt` が存在する場合はそのファイル
4. `Chise diagnostics event=...` の直近行
5. Minecraft / Fabric Loader / Fabric API / Java version

### `disconnect-*-client.txt` はCrash Reportではない

`disconnect-*-client.txt` はサーバー接続が切れただけでも生成されます。たとえば次の例外だけではChiseTweaksのクライアントクラッシュとは断定できません。

```text
java.net.SocketException: Connection reset
```

Prism logは次でも機械判定できます。

```bash
python scripts/prism_acceptance_audit.py /path/to/latest.log --require-join --require-disconnect
```

`Connection reset` はtransport disconnectとして集計し、Mixin failure / component quarantine / resource reload failureとは分けて扱います。

## 安全性・互換性

| 方針 | 状態 |
| --- | --- |
| Client only | ✅ |
| サーバーMOD不要 | ✅ |
| custom Play Protocol | なし |
| 独自packet送信 | なし |
| remote MOD detection | なし |
| ChiseTweaks独自background thread | なし |
| 自動MOD download | なし |
| 自動JAR replacement | なし |
| Mixin | `required=false` / fail-soft |
| Feature障害 | quarantineで隔離 |
| Analyzer | 未ロードチャンクを要求しない |
| Sodium | 任意・推奨 / hard dependencyではない |

Sodium / Iris / ImmediatelyFast / EntityCullingを `depends / breaks / conflicts` に入れず、renderer MOD実装namespaceへproduction Java / resource metadataから直接結合しないことをCIで監査します。

## パフォーマンス方針

通常利用時の原則:

- loaded chunks onlyの探索
- 固定上限bufferとretained GPU bufferを優先
- resource reload完了待ちで `.join()` / `.get()` を使用しない
- Ancient Debris bootstrapは最大 `64 chunks / tick`
- Ancient Debris validationは最大 `16 chunks / tick`
- runtime iconは `128x128`
- built-in pack iconは `64x64`
- 容量削減よりFunctional Parity、起動安定性、互換性、診断可能性を優先

容量Gate:

- runtime JAR最終目標: `358400 bytes` 以下（350 KiB）
- M0 frozen size baseline: `446814 bytes`
- 軽量化中に許容する容量増加: `0 bytes`
- absolute / effective CI上限: `446814 bytes`

### Prism性能比較

固定scenario:

- `chise-absent`
- `chise-all-off`
- `analyzers-on`
- `highlights-on`
- `maximum-supported-load`

空のbaseline / candidate CSV:

```bash
python scripts/performance_evidence_template.py performance-evidence --environment-id my-prism-pc
```

同一scenario / 同一環境で最低3回ずつ採取し、必要に応じてJFRとともに比較します。

```bash
./gradlew comparePerformanceEvidence \
  -PperformanceBaseline=performance-evidence/chise-all-off-baseline-0.9.3.csv \
  -PperformanceCandidate=performance-evidence/chise-all-off-candidate-0.9.4.csv
```

評価対象はstartup、P50/P95/P99 frametime、heap、allocation、render-thread CPU、average FPSです。実測手順は `docs/performance/0.9.4-baseline.md` を正とします。

## 自動品質ゲート

CIでは次をRelease Candidateの必須条件として扱います。

- Java 25 / Gradle / Python toolchain contract
- version policy
- repository / production source audit
- README / version / Fabric metadata consistency audit
- **Functional Parity Audit**（FeatureDefinition、設定key/default、Resource Pack ID、Mixin、Fabric契約、UI action、diagnostics、migration、Analyzer budget）
- optional renderer compatibility contract audit
- blocking reload / force chunk load / Analyzer budget contract audit
- Java compile warning = 0
- JUnit
- JaCoCo line coverage gate
- PIT mutation coverage / mutation score / test strength gate
- Artifact Audit
- Visual Asset Audit
- Release Residue Audit
- runtime JAR no-growth size / metadata / client-only contract

品質閾値はJaCoCo line coverage、PIT coverage、mutation score、test strengthの各 `96%` です。

## 実機受入

自動テストではMinecraftの実GPU描画、Prism Launcher固有環境、実際のworld移動時の見た目やframetimeを完全には再現できません。

正式配布前の実機受入では `docs/acceptance/0.9.4-prism.md` に従い、少なくとも次を確認対象とします。

- 11 runtime features同時ON
- Fine Line + Hidden Block + Nether Highlight同時ON
- Block / Entity Visual Filter
- Bright Chest / Bright Concrete高速ON/OFF
- Lava Analyzer + Ancient Debris Analyzer同時利用
- 古代の残骸の追加 / 削除反映
- Overworld / Nether / End移動
- disconnect / reconnect
- Prism Launcher / `latest.log` のstructured diagnostics
- 大規模建築環境でのFPS / frametime / heap比較

> [!CAUTION]
> **このREADMEは、実機操作や実測Performanceを実施済みと主張するものではありません。** 実機受入結果は自動品質ゲートと分けて扱います。

## Build / Test

```bash
./gradlew clean test jacocoTestCoverageVerification pitest assemble
python scripts/repository_audit.py
python scripts/source_usage_audit.py
python scripts/documentation_consistency_audit.py
python scripts/functional_parity_audit.py
python scripts/compatibility_contract_audit.py
python scripts/runtime_performance_contract_audit.py
python scripts/artifact_audit.py
python scripts/visual_asset_audit.py
python scripts/release_residue_audit.py
```

配布物はruntime JARのみです。sources JARは品質検証・開発用artifactとして生成されますが、Official Releaseへアップロードする対象ではありません。
