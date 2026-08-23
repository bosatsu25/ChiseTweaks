# ChiseTweaks

ChiseTweaks は、大規模建築・技術施設の確認作業を支援する **Fabric クライアント専用** Minecraft MOD です。

サーバー側への導入、独自通信プロトコル、自動MODダウンロード、自動JAR置換は行いません。Minecraftクライアントの描画・モデル・ローカル設定・標準Resource Packs機構の範囲で動作します。

## 対応環境

| 項目 | 対応 |
| --- | --- |
| ChiseTweaks | `0.9.2+mc26.1.2` |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 動作側 | Client only |
| サーバー導入 | 不要 |
| Mod Menu | 任意 |
| Sodium | 任意・推奨 |

> 配布JARは Minecraft `26.1.2` 専用です。異なるMinecraft / Fabric / Javaバージョンへ流用しないでください。

## 現在の機能構成

設定画面上では **Highlight / Visual Filter / Analyzer / Visibility** の4系統に整理されています。

主要な11個のruntime featureは初期状態ではOFFです。Chest VisibilityとWhite Concrete VisibilityはMinecraft標準Resource Packsとして初期ONです。

## Highlight

### Ore Highlights
鉱石・古代の残骸・黒曜石系を、元のブロックモデルを維持したままChiseTweaksのオーバーレイで強調します。

- 通常鉱石 / 深層岩鉱石を鉱石ファミリー単位で管理
- 対象鉱石を個別ON/OFF可能
- 静止表示 / 控えめなアニメーションを切替可能
- Modded Ore互換設定に対応
- ワールド全体を走査するX-ray方式ではなく通常のモデル描画経路を利用

### Nether Highlight
見えているネザー建築素材を、ローカルな範囲内で色分けした補助線として表示します。

### Fine Line Highlight
トリップワイヤーとトリップワイヤーフックなど、細い技術ブロックを見つけやすくします。

### Hidden Block Highlight
Powder Snow / Blue Ice / Dead Coral / Sculk Catalystなど、見落としやすい対象を補助線で強調します。

### Glass Highlight
ガラスブロックと板ガラスへ形状別のフルブライト補助表示を追加します。既存resource packのベースモデルと色は維持します。

### Kelp Highlight
Kelp / Kelp Plantへマゼンタ＋オレンジのモデルベースオーバーレイを追加します。

### Highlightの同時利用

**Fine Line Highlight / Hidden Block Highlight / Nether Highlightは同時にONにできます。**

旧実装に存在した排他モード、片方をONにすると別機能がOFFになる連動処理、旧result上限は削除済みです。各機能のON/OFFは独立しています。

共通設定:

- 水平走査範囲: `1–8`
- 垂直走査範囲: `1–5`
- 更新間隔: `5–100 ticks`
- 最大オーバーレイ数: `1–24`
- ディメンション自動プリセット
- Fine Line / Hidden Blockの色と不透明度
- 各ハイライトの対象ブロック

## Visual Filter

建築確認や撮影時に、指定したブロック・エンティティの表示を一時的に絞り込みます。

### Block Filter
ブロックIDのAllow list / Hide listを設定し、対象ブロックの描画を制御します。必要に応じて表示中チャンクの再描画を要求できます。

### Entity Filter
エンティティIDのAllow list / Hide listを設定し、対象エンティティの描画を制御します。プレイヤー自身はフィルター対象から保護されます。

Visual Filterはクライアント描画だけを変更し、サーバー側のブロック・エンティティ状態は変更しません。

## Analyzer

### Lava Source Highlight
読み込み済みチャンク内の近距離だけを対象に溶岩源を走査し、地形越しに **輪郭＋半透明面** を表示します。

- Flowing Lavaは対象外
- 未ロードチャンクを強制ロードしない
- 水平 / 垂直範囲、更新間隔、最大表示数を個別設定
- 保持型GPUバッファを利用
- 距離に応じて深緑系の表示を変化

### Ancient Debris Analyzer
ネザー内の **すでに読み込まれているクライアントチャンク** だけを対象に古代の残骸を検出し、地形越しに **輪郭＋半透明面** を表示します。

- Nether only
- 検出範囲: `16–256 blocks`
- 最大表示数: `8–128`
- 未ロードチャンクを要求しない
- 初回探索は複数tickへ分散
- 追跡済みチャンクを固定予算で再走査し、後から追加・削除された古代の残骸も反映
- 再検証: 最大 `16 chunks / tick`
- 最大追跡数: `4096 chunks`
- 1チャンクあたり最大保持数: `256`
- ワールド退出 / 再接続 / ディメンション切替時にsession cacheを破棄

## Visibility

### Lower Fire Overlay / Fire Visibility
プレイヤーが燃えているときの **一人称炎オーバーレイだけ** を下げ、中央付近の視界を確保します。

ワールド上の炎モデル、炎テクスチャ、サーバー状態は変更しません。

### Chest Visibility / チェスト視認性
チェスト、ダブルチェスト左右の高視認テクスチャを切り替えます。

### White Concrete Visibility / 白色コンクリート視認性
White Concreteの高視認テクスチャを切り替えます。

Chest VisibilityとWhite Concrete Visibilityは **完全に別のbuilt-in resource pack** です。片方をOFFにしても、もう片方の状態は変わりません。

内蔵pack:

- `chisetweaks:chise_chest_visibility`
  - Chest
  - Double Chest Left
  - Double Chest Right
- `chisetweaks:chise_white_concrete_visibility`
  - White Concrete

どちらも初期ONで、Minecraftの **Options → Resource Packs** から通常のresource packとして直接切り替えることもできます。ゲーム内テクスチャのバイト列は統合元と同一で、pack iconだけ`64x64`へ軽量化しています。

2つのpackは同じresource-reload queueを共有します。高速にON/OFFしてもreloadを重複実行せず、最後に要求された選択状態へ収束します。非同期reload失敗時には既知の正常状態へrollbackします。

## 導入方法

### Prism Launcher

1. Minecraft `26.1.2` / Fabricのインスタンスを用意します。
2. Java `25` を使用するようPrism Launcher側で設定します。
3. Fabric APIを導入します。
4. GitHub Releasesから対象バージョンの `chise-tweaks-<version>.jar` を取得します。
5. Prism Launcherの **Mods** に、そのJARを1個だけ追加します。
6. Minecraftを起動します。

ChiseTweaksはクライアント専用なので、Minecraftサーバーの `mods` フォルダへ入れる必要はありません。

## 設定

Mod Menuを導入している場合は、Mod MenuからChiseTweaksの設定画面を開けます。Mod Menu自体は必須依存ではありません。

ローカル設定はMinecraftインスタンスの `config` ディレクトリに保存されます。

- `chisetweaks.json`
- `chisetweaks-visual.json`

設定JSONは読み込み時に検証・サニタイズされ、不正または安全でない内容はそのまま適用しません。

古いバージョンに存在した旧Highlight排他モードや旧result上限などは現行スキーマでは使用しません。未知の古いフィールドは実行状態へ反映されず、現行設定で保存し直した際に整理されます。

設定画面では変更内容をメモリ上で扱い、**Apply / Done** を永続化境界として保存します。

Chest / White Concreteの有効状態はMinecraft標準Resource Packs管理を正とするため、ChiseTweaksのJSONへ重複保存しません。

## 安全性・障害分離

- サーバーMOD不要
- カスタムPlay Protocolなし
- サーバーへの独自packet送信なし
- リモートMOD検出なし
- ChiseTweaks自身による独自バックグラウンドスレッド生成なし
- 自動MODダウンロードなし
- 自動JAR置換なし
- 外部設定ライブラリ不要
- Feature障害はquarantine方式で隔離
- Mixin障害はfail-softを基本方針とする
- Analyzerは未ロードチャンクを生成・要求しない

描画機能の一部で障害が発生した場合も、可能な限り該当機能だけを隔離し、Minecraftクライアント全体への波及を抑えます。

## パフォーマンス方針

大規模建築・技術施設での利用を想定し、処理量に明示的な上限を設けています。

- Highlight scan candidate上限: `128`
- 1走査あたりのLine-of-Sight ray上限: `192`
- World overlay上限: `24`
- Ancient Debris初回探索を複数tickへ分散
- Ancient Debris再検証: 最大 `16 chunks / tick`
- 壁越しAnalyzer描画は保持型GPUバッファを再利用
- 描画対象Snapshotと候補bufferは固定上限で再利用
- フレームごとの不要なジオメトリ再生成・大規模allocationを回避
- runtime JAR icon: `128x128`
- built-in resource pack icon: `64x64`
- runtime JAR: `400KB`未満を改善目標、`440KB`未満をCI上の回帰上限として監視

## 実機パフォーマンス受入

GPUドライバ、Sodium、描画距離、建築規模の影響を受けるため、実FPSをGitHub CI上の疑似値で代用しません。実際のPrism Launcher環境で同一条件のbaseline / candidateをそれぞれ3回以上取得し、テスト専用CLIで判定します。

```text
./gradlew comparePerformanceEvidence \
  -PperformanceBaseline=<baseline.csv> \
  -PperformanceCandidate=<candidate.csv>
```

JFRも比較する場合:

```text
./gradlew comparePerformanceEvidence \
  -PperformanceBaseline=<baseline.csv> \
  -PperformanceCandidate=<candidate.csv> \
  -PperformanceBaselineJfr=<baseline.jfr> \
  -PperformanceCandidateJfr=<candidate.jfr>
```

同一 `scenario / minecraft / java / fabric_loader / environment_id` のcaptureだけを比較できます。受入判定では少なくともP50 / P95 / P99 frametime、heap、average FPSの3回以上の測定を必須とします。

回帰許容上限:

| 指標 | 最大悪化率 |
| --- | ---: |
| P50 frametime | 8% |
| P95 frametime | 10% |
| P99 frametime | 15% |
| Heap | 10% |
| Average FPS | 10%低下 |
| Allocation | 15% |
| Render thread CPU | 10% |
| Startup | 15% |

上限を超えるとCLIは `performance_acceptance=FAIL` で失敗します。範囲内なら `performance_acceptance=PASS` です。

## 0.9.x統合回帰

CIでは次の不変条件も固定しています。

- 11個のruntime featureを同時にONにできる
- 1機能を切り替えても他機能の設定が勝手に反転しない
- Visual Filter / Fine Line / Hidden Block / Nether Highlightに旧排他処理が戻らない
- Chest / White Concreteのpack selectionが独立している
- rapid toggleのreload state machineが最終要求へ収束する
- join / disconnect / dimension-sensitive featureにsession reset経路が存在する
- Ancient Debrisの追加・削除を追跡済みチャンクの再走査で反映する
- `256 blocks / 128 markers` を超えない

## 品質保証

CIはJava 25環境で以下を実行します。

- コンパイル
- JUnit contract / regression tests
- JaCoCo line coverage gate
- PIT mutation / test-strength gate
- Repository audit
- Production source usage audit
- Artifact audit
- Visual asset audit
- Release residue audit
- client-only metadata検証
- runtime JAR容量検証
- split built-in visibility packsの構成・PNG寸法・元テクスチャSHA-256検証

品質ゲート対象のcoverage / mutation / test-strengthは現在 `96%` を基準にしています。

## Release

Releaseは `main` のCIが成功した場合のみ自動実行され、CIで検証したcommit SHAと `main` の最新SHAが一致する場合だけ進みます。

Release workflowは以下を再確認します。

- version / SemVer
- 既存tag・既存Releaseとの衝突防止
- runtime JAR再ビルド
- artifact / visual asset / release residue監査
- SHA-256
- `fabric.mod.json`
- Minecraft / Fabric / Java条件
- client-only条件

GitHub ReleaseへChiseTweaksがアップロードするMOD成果物は **runtime `.jar` 1個だけ**です。GitHubが自動生成するSource code ZIP / tar.gzはGitHub側の標準表示です。

## バージョニング

形式:

```text
MAJOR.MINOR.PATCH+mc<MinecraftVersion>
```

現在:

```text
0.9.2+mc26.1.2
```

- `PATCH`: バグ修正・互換性を壊さない改善
- `MINOR`: 互換性を維持した機能追加
- `MAJOR`: 互換性を壊す変更

同じversion/tagのReleaseは再生成しません。

## Releaseから使うファイル

Minecraftへ導入するのは次のJARだけです。

```text
chise-tweaks-0.9.2+mc26.1.2.jar
```

## ライセンス

このリポジトリには `MIT` / `Apache-2.0` のライセンス情報とNOTICEが含まれます。再配布・公開範囲を変更する場合は、リポジトリ内のLICENSE / NOTICEを確認してください。
