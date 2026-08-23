# ChiseTweaks

ChiseTweaks は、大規模建築・技術施設の確認作業を支援する **Fabric クライアント専用** Minecraft MOD です。

サーバー側への導入、独自通信プロトコル、自動MODダウンロード、自動JAR置換は行いません。Minecraftクライアントの描画・モデル・ローカル設定・標準Resource Packs機構の範囲で動作します。

## 対応環境

| 項目 | 対応 |
| --- | --- |
| ChiseTweaks | `0.9.1+mc26.1.2` |
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

ChiseTweaksの主要機能は、設定画面上で **Highlight / Visual Filter / Analyzer / Visibility** の4系統に整理されています。

主要な11機能は初期状態ではOFFです。必要な機能だけを有効化してください。

### Highlight

#### Ore Highlights
鉱石・古代の残骸・黒曜石系を、元のブロックモデルを残したままChiseTweaksのオーバーレイで強調します。

- 通常鉱石 / 深層岩鉱石を鉱石ファミリー単位で管理
- 対象鉱石を個別ON/OFF可能
- 静止表示 / 控えめなアニメーションを切替可能
- Modded Oreの互換設定に対応
- ワールド全体を走査するX-ray方式ではなく、通常のモデル描画経路を利用

#### Nether Highlight
見えているネザー建築素材を、ローカルな範囲内で色分けした補助線として表示します。

#### Fine Line Highlight
トリップワイヤーとトリップワイヤーフックなど、細い技術ブロックを見つけやすくします。

#### Hidden Block Highlight
見落としやすい対象ブロックを補助線で強調します。現在の対象には Powder Snow / Blue Ice / Dead Coral / Sculk Catalyst が含まれます。

#### Glass Highlight
ガラスブロックと板ガラスに形状別のフルブライト補助表示を追加します。既存resource packのベースモデルと色は維持します。

#### Kelp Highlight
Kelp / Kelp Plantへマゼンタ＋オレンジのモデルベースオーバーレイを追加します。

### Highlightの同時利用

**Fine Line Highlight / Hidden Block Highlight / Nether Highlight は同時にONにできます。**

旧実装に存在した排他モードと、片方をONにすると別機能がOFFになる連動処理は削除されています。複数機能を同時に有効化しても設定値を相互に書き換えません。

共通設定では次を調整できます。

- 水平走査範囲: `1–8`
- 垂直走査範囲: `1–5`
- 更新間隔: `5–100 ticks`
- 最大オーバーレイ数: `1–24`
- ディメンション自動プリセット
- Fine Line / Hidden Blockの色と不透明度
- 各ハイライトの対象ブロック

## Visual Filter

建築確認や撮影時に、指定したブロック・エンティティの表示を一時的に絞り込む機能です。

### Block Filter
ブロックIDの Allow list / Hide list を設定し、対象ブロックの描画を制御します。必要に応じて表示中チャンクの再描画を要求できます。

### Entity Filter
エンティティIDの Allow list / Hide list を設定し、対象エンティティの描画を制御します。プレイヤー自身はフィルター対象から保護されます。

Visual Filterはクライアント描画だけを変更し、サーバー側のブロック・エンティティ状態は変更しません。

## Analyzer

### Lava Source Highlight
読み込み済みチャンク内の近距離だけを対象に溶岩源を走査し、地形越しに輪郭＋半透明面を表示します。

- Flowing Lavaは対象外
- 未ロードチャンクを強制ロードしない
- 水平 / 垂直範囲、更新間隔、最大表示数を個別設定
- 保持型GPUバッファを利用
- 距離に応じて深緑系の表示を変化

### Ancient Debris Analyzer
ネザー内の **すでに読み込まれているクライアントチャンク** だけを対象に古代の残骸を検出し、地形越しに輪郭＋半透明面を表示します。

- Nether only
- 検出範囲: `16–256 blocks`
- 最大表示数: `8–128`
- 未ロードチャンクを要求しない
- 初回探索は複数tickへ分散
- 追跡済みチャンクも固定予算で再走査し、後から追加・削除された古代の残骸を反映
- 再検証は最大 `16 chunks / tick`
- 最大追跡数 `4096 chunks`
- 1チャンクあたり最大 `256` 個を保持

## Visibility

### Lower Fire Overlay / Fire Visibility
プレイヤーが燃えているときの **一人称炎オーバーレイだけ** を下げ、中央付近の視界を確保します。

ワールド上の炎モデル、炎テクスチャ、サーバー状態は変更しません。

### Chest Visibility / チェスト視認性
ChiseTweaks内蔵の `Chise Texture` resource packを、ゲーム内設定からON/OFFできます。

現在のbuilt-in packには次の4テクスチャが含まれます。

- White Concrete
- Chest
- Double Chest Left
- Double Chest Right

そのため、**Chest Visibilityの切替ではチェストだけでなくWhite Concreteも同時に切り替わります。** これは現在のresource pack構成上の仕様です。

外部resource-pack ZIPをPrism Launcherへ追加する必要はありません。Minecraftの **Options → Resource Packs** から通常のresource packとして直接切り替えることもできます。

Chise Textureは初期状態で有効です。ゲーム内テクスチャ4枚は統合元と同一バイトを維持し、`pack.png`だけ64x64へ軽量化しています。

resource reloadは連打時に重複実行しないよう集約され、非同期reloadが失敗した場合は選択状態を直前の状態へ戻すようにしています。

## 導入方法

### Prism Launcher

1. Minecraft `26.1.2` / Fabric のインスタンスを用意します。
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

古いバージョンに存在した廃止済み設定値（旧Highlight排他モード、旧result上限など）は現行スキーマでは使用しません。未知の古いフィールドは実行状態へ反映されず、現行設定で保存し直したときに整理されます。

設定画面では変更内容をメモリ上で扱い、**Apply / Done** を永続化境界として保存します。

Chise Textureの有効状態はMinecraft標準のResource Packs管理を正とするため、ChiseTweaksのJSONには同じ状態を重複保存しません。

## 安全性・障害分離

ChiseTweaksはクライアント単体で完結することを前提にしています。

- サーバーMOD不要
- カスタムPlay Protocolなし
- サーバーへの独自packet送信なし
- リモートMOD検出なし
- ChiseTweaks自身による独自バックグラウンドスレッド生成なし
- 自動MODダウンロードなし
- 自動JAR置換なし
- 外部設定ライブラリ不要
- Mod Menuは任意
- Feature障害はquarantine方式で隔離
- Mixin障害はfail-softを基本方針とする
- Analyzerは未ロードチャンクを生成・要求しない

描画機能の一部で障害が発生した場合も、可能な限り該当機能だけを隔離し、Minecraftクライアント全体への波及を抑える設計です。

Chise TextureはvanillaテクスチャをMOD直下から常時上書きせず、独立したbuilt-in resource packとして登録します。他resource packとの優先順位はMinecraft標準のResource Packs画面で管理できます。

## パフォーマンス方針

大規模建築・技術施設での利用を想定し、処理量に明示的な上限を設けています。

- Highlightのローカル走査は読み込み済みチャンクだけを対象
- Worksite scan candidate上限: `128`
- 1走査あたりのLine-of-Sight ray上限: `192`
- World overlay上限: `24`
- Ancient Debris初回探索は固定上限で複数tickへ分散
- Ancient Debris再検証は最大 `16 chunks / tick`
- 壁越しAnalyzer描画は保持型GPUバッファを再利用
- 描画対象Snapshotと候補bufferは固定上限で再利用
- フレームごとの不要なジオメトリ再生成・大規模allocationを避ける
- runtime JARのアイコンは配布時のみ `128x128` へ正規化
- Chise Textureのpack iconは `64x64`
- runtime JARは `400KB` 未満を改善目標、`440KB` 未満をCI上の回帰上限として監視

`400KB` は配布容量の改善目標であり、FPS向上を直接保証する値ではありません。機能・安全性・デバッグ性を削って容量だけを小さくする方針は採用していません。

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
- built-in Chise Textureの登録方式・PNG寸法・元テクスチャSHA-256検証

品質ゲート対象のcoverage / mutation / test-strengthは現在 `96%` を基準にしています。

Releaseは `main` のCIが成功した場合のみ自動実行され、CIで検証したものと同じcommit SHAを対象に再ビルド・再監査します。

GitHub ReleaseへChiseTweaksがアップロードするMOD成果物は **runtime `.jar` 1個だけ**です。GitHubが自動生成するSource code ZIP / tar.gzはGitHub側の標準表示です。

## 0.9.1 の主な変更

- Visual Filterを利用可能化
- Block / Entity Filterを設定画面へ公開
- Fine Line / Hidden Block / Nether Highlightの排他制御を廃止し、同時ONを許可
- Lower Fire Overlayを利用可能化
- Chest VisibilityをVisibilityセクションへ追加
- Chise Textureのreload競合・非同期失敗時rollbackを改善
- Lava Source / Ancient Debrisの壁越しマーカーへ半透明面を追加
- Ancient Debrisの追跡済みチャンク再検出漏れを修正
- Ancient Debris再走査を固定budget化
- 旧Highlight排他設定・旧result設定・不要policyを削除
- 描画geometryの反転を防ぐ回帰ガードを追加

## バージョニング

形式:

```text
MAJOR.MINOR.PATCH+mc<MinecraftVersion>
```

現在のソースバージョン:

```text
0.9.1+mc26.1.2
```

- `PATCH`: バグ修正・互換性を壊さない改善
- `MINOR`: 互換性を維持した機能追加
- `MAJOR`: 互換性を壊す変更

同じversion/tagのReleaseは再生成しません。新しい正式JARを公開する場合はSemVerを更新します。

## Releaseから使うファイル

Minecraftへ導入するのはruntime JARだけです。現行ソースバージョンをReleaseする場合のファイル名は次の形式です。

```text
chise-tweaks-0.9.1+mc26.1.2.jar
```

Release workflowは公開後に、asset数、ファイル名、SHA-256、`fabric.mod.json`、Minecraft / Fabric / Java条件、client-only条件を再検証します。

## ライセンス

このリポジトリには `MIT` / `Apache-2.0` のライセンス情報とNOTICEが含まれます。再配布・公開範囲を変更する場合は、リポジトリ内のLICENSE / NOTICEを確認してください。
