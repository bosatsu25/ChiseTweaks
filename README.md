# ChiseTweaks

ChiseTweaks は、大規模建築・技術施設の確認作業を支援する **Fabric クライアント専用** Minecraft MOD です。

サーバー側への導入、独自通信プロトコル、自動MODダウンロード、自動JAR置換は行いません。Minecraftクライアントの描画・モデル・ローカル設定・標準Resource Packs機構の範囲で動作します。

## 対応環境

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

> 配布JARは Minecraft `26.1.2` 専用です。異なるMinecraft / Fabric / Javaバージョンへ流用しないでください。

## 0.9.4 の主な更新

`0.9.4+mc26.1.2` は、0.9.3で固めたmigration / reload / diagnostics基盤を、Prism実機受入と性能回帰の切り分けまで扱える形へ拡張します。

- 設定画面へ現在のResource Reload状態を表示
- runtime diagnostic snapshotをClipboardへコピー可能
- diagnostic snapshotを `config/chisetweaks/diagnostics/` へ保存可能
- diagnostic fileは最大10件へ自動整理し、サーバーアドレス・ユーザー名・絶対pathを追加収集しない
- `Connection reset` をChiseTweaks内部failureと混同しないPrism log auditを追加
- `0.7.7 / 0.9.1 / 0.9.2 / future schema` のconfig fixtureを固定し、migration / downgrade safetyを継続検証
- Resource Reload Coordinatorへ長時間状態遷移stress regressionを追加
- Ancient Debris Analyzerへ長時間budget / retained-capacity regressionを追加
- Sodium / Iris / ImmediatelyFast / EntityCullingについてhard dependency・conflict・implementation namespace直結をCIで禁止
- resource reloadのblocking wait、Analyzerのforce chunk load、budget逸脱をruntime performance contract auditで検出
- 5つのPrism性能scenario用baseline/candidate CSV template generatorを追加
- 検証済み0.9.4 JAR `446814 bytes` をM0 size baselineとして固定し、軽量化中の容量増加を拒否
- runtime JARの最終目標を `358400 bytes`（350 KiB）以下へ固定。ただしFunctional Parityを優先し、機能削除による達成は認めない
- `RuntimeDiagnosticReport`をJaCoCo / PITの96% retained-scope gateへ追加

## 現在の機能構成

設定画面は **Highlight / Visual Filter / Analyzer / Visibility** の4系統に、診断操作を加えた構成です。主要な11個のruntime featureは初期状態OFFです。Chest VisibilityとWhite Concrete VisibilityはMinecraft標準Resource Packsとして初期ONです。

### Highlight

- **Ore Highlights**: 鉱石・古代の残骸・黒曜石系をモデル描画経路で強調。鉱石family、個別target、animation、modded ore互換設定に対応。
- **Nether Highlight**: ネザー建築素材をローカル範囲で補助表示。
- **Fine Line Highlight**: Tripwire / Tripwire Hookなど細い技術ブロックを強調。
- **Hidden Block Highlight**: Powder Snow / Blue Ice / Dead Coral / Sculk Catalystなどを補助表示。
- **Glass Highlight**: Glass / Glass Paneを形状別に強調。
- **Kelp Highlight**: Kelp / Kelp Plantへモデルベースの視認性オーバーレイを追加。

Fine Line / Hidden Block / Nether Highlightは同時にONにできます。旧排他モードや連動OFF処理は使用しません。

共通のHighlight設定は水平範囲 `1–8`、垂直範囲 `1–5`、更新間隔 `5–100 ticks`、最大オーバーレイ `1–24` の範囲へ制限されます。

### Visual Filter

- **Block Filter**: Block IDのAllow / Hide listでローカル描画を制御。
- **Entity Filter**: Entity IDのAllow / Hide listでローカル描画を制御。プレイヤー自身は保護。

Visual Filterはクライアント描画だけを変更し、サーバー側のblock/entity状態は変更しません。

### Analyzer

**Lava Source Highlight** は読み込み済みチャンクの近距離だけを走査し、溶岩源を輪郭＋半透明面で表示します。Flowing Lavaは対象外で、未ロードチャンクを強制ロードしません。

**Ancient Debris Analyzer** はNether内の読み込み済みクライアントチャンクだけを対象に古代の残骸を検出します。

- 検出範囲: `16–256 blocks`
- 最大表示数: `8–128`
- 初回探索: 最大 `64 chunks / tick`
- 再検証: 最大 `16 chunks / tick`
- 最大追跡数: `4096 chunks`
- 1チャンクあたり最大保持数: `256`
- 初回探索は複数tickへ分散
- 追跡済みチャンクを定期再走査し、後から追加・削除された古代の残骸も反映
- disconnect / reconnect / dimension変更時にsession cacheを破棄

### Visibility

**Fire Visibility** は一人称の炎オーバーレイだけを下げます。ワールド上の炎モデルやサーバー状態は変更しません。

Chest / White Concreteは完全に独立したbuilt-in resource packです。

- `chisetweaks:chise_chest_visibility`
  - Chest
  - Double Chest Left
  - Double Chest Right
- `chisetweaks:chise_white_concrete_visibility`
  - White Concrete

2つのpackは同一のserialized resource-reload queueを共有します。高速に切り替えてもreloadを重複実行せず、最後の要求状態へ収束します。reload失敗時は既知の正常選択へrollbackし、client-threadへcompletionを配送できなかった場合はterminal recoveryを次の安全なsession境界へ保持します。

## 0.9.1以前からのVisibility pack移行

旧 `chisetweaks:chise_texture` を使用していたインスタンスでは、初回起動時に現在のChest / White Concrete packへ一度だけ移行します。

migrationは次を区別します。

- 旧packが有効だった既存インスタンス
- 旧packを無効化していた既存インスタンス
- 0.9.2以降ですでに2packを個別変更済みのインスタンス
- 新規インストール
- migration済みインスタンス

無関係なResource Packの順序や選択状態は変更しません。migration完了markerにより同じ移行を繰り返しません。

## 設定

Mod Menuは任意です。導入している場合はMod MenuからChiseTweaks設定画面を開けます。

設定ファイル:

- `config/chisetweaks.json`
- `config/chisetweaks-visual.json`

設定JSONは検証・サニタイズされ、未知の古いfieldはruntime状態へ直接反映しません。Visual Target schemaは既知のversionから移行し、future schemaを読み込んだ場合も現行target maskへ安全に制限します。

設定画面では変更をメモリ上で扱い、**Apply / Done** を永続化境界とします。Chest / White Concreteの状態はMinecraft標準Resource Packsを正とし、ChiseTweaks JSONへ重複保存しません。

## Structured Diagnostics

ChiseTweaksは異常時の原因切り分け用に、必要最小限のruntime状態を単一行で出力します。

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

`reloadState` は `idle / reloading / recovery_pending / reloading_with_recovery` のいずれかです。設定画面でも現在値を確認できます。

設定画面の **Copy Diagnostic Snapshot** では同じsnapshotをClipboardへコピーできます。**Export Diagnostic Snapshot** では `config/chisetweaks/diagnostics/` に保存し、ChiseTweaks生成reportだけを最大10件保持します。

対象eventにはstartup / join / disconnect、component quarantine、resource selection/reload/recovery、Visibility pack migrationがあります。detailは長さと文字種を制限し、改行や制御文字をそのままログへ入れません。サーバーアドレス、ユーザー名、ローカル絶対pathなどはdiagnostic snapshotへ収集しません。

## トラブルシューティング

### `disconnect-*-client.txt` とCrash Reportを分ける

`disconnect-*-client.txt` はサーバー接続が切れた場合にも生成されます。たとえば次の例外だけではChiseTweaksのクライアントクラッシュとは断定できません。

```text
java.net.SocketException: Connection reset
```

Minecraftプロセス自体が終了した、画面が固まった、描画例外が発生した場合は次を確認してください。

1. `logs/latest.log`
2. `crash-reports/crash-*.txt` が存在する場合はそのファイル
3. `disconnect-*-client.txt` が存在する場合はそのファイル
4. `Chise diagnostics event=...` の直近行またはExportしたdiagnostic snapshot
5. Minecraft / Fabric Loader / Fabric API / Java version

Prism logは次でも機械判定できます。

```bash
python scripts/prism_acceptance_audit.py /path/to/latest.log --require-join --require-disconnect
```

`Connection reset` はtransport disconnectとして集計し、Mixin failure / component quarantine / resource reload failureとは分けて扱います。

## 安全性・互換性

- Client only
- サーバーMOD不要
- custom Play Protocolなし
- 独自packet送信なし
- remote MOD detectionなし
- ChiseTweaks独自background threadなし
- 自動MOD downloadなし
- 自動JAR replacementなし
- Mixinは`required=false`のfail-soft構成
- Feature障害はquarantineで隔離
- Analyzerは未ロードチャンクを要求しない
- Sodiumは任意・推奨でありhard dependencyではない
- Sodium / Iris / ImmediatelyFast / EntityCullingを `depends / breaks / conflicts` に入れない
- renderer MOD実装namespaceへproduction Java / resource metadataから直接結合しないことをCIで監査

## パフォーマンス方針

- loaded chunks onlyの探索
- 固定上限bufferとretained GPU bufferを優先
- resource reload完了待ちで `.join()` / `.get()` を使用しない
- Ancient Debris bootstrapは最大 `64 chunks / tick`
- Ancient Debris validationは最大 `16 chunks / tick`
- runtime iconは `128x128`
- built-in pack iconは `64x64`
- runtime JAR最終目標: `358400 bytes` 以下（350 KiB）
- M0 frozen size baseline: `446814 bytes`
- 軽量化中に許容する容量増加: `0 bytes`
- absolute / effective CI上限: `446814 bytes`
- 容量削減よりFunctional Parity、起動安定性、互換性、診断可能性を優先

Prismでの性能比較は次の5 scenarioを固定します。

- `chise-absent`
- `chise-all-off`
- `analyzers-on`
- `highlights-on`
- `maximum-supported-load`

空のbaseline / candidate CSVは次で生成できます。

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

## 実機受入について

自動テストではMinecraftの実GPU描画、Prism Launcher固有環境、実際のworld移動時の見た目やframetimeを完全には再現できません。正式配布前の実機受入では `docs/acceptance/0.9.4-prism.md` に従い、少なくとも次を確認対象とします。

- 11 runtime features同時ON
- Fine Line + Hidden Block + Nether Highlight同時ON
- Block / Entity Visual Filter
- Chest / White Concrete高速ON/OFF
- Lava + Ancient Debris同時利用
- 古代の残骸の追加 / 削除反映
- Overworld / Nether / End移動
- disconnect / reconnect
- diagnostic copy / exportとreload状態表示
- 大規模建築環境でのFPS / frametime / heap比較

**このREADMEは、実機操作や実測Performanceを実施済みと主張するものではありません。** 実機受入結果は自動品質ゲートと分けて扱います。

## 導入

Prism LauncherではMinecraft `26.1.2` / Fabric / Java `25`のインスタンスを使用し、Fabric APIを導入したうえで、Modsへ次のruntime JARを1個だけ追加します。

`chise-tweaks-0.9.4+mc26.1.2.jar`

ChiseTweaksはクライアント専用なのでサーバーの`mods`フォルダへ入れる必要はありません。

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