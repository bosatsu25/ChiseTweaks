# ChiseTweaks

ChiseTweaks は、大規模建築・技術施設の確認作業を支援する **Fabric クライアント専用** Minecraft MOD です。

サーバー側への導入、独自通信プロトコル、自動MODダウンロード、自動JAR置換は行いません。Minecraftのクライアント描画とローカル設定だけで動作します。

## 対応環境

| 項目 | 対応 |
| --- | --- |
| ChiseTweaks | `0.9.0+mc26.1.2` |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 動作側 | Client only |
| サーバー導入 | 不要 |
| Mod Menu | 任意 |
| Sodium | 任意・推奨 |

> 配布JARは Minecraft `26.1.2` 専用です。異なるMinecraft/Fabric/Javaバージョンへ流用しないでください。

## 現在利用可能な機能

### Ore Highlights
鉱石系ブロックを視認しやすくするハイライト機能です。通常鉱石と深層岩鉱石を含む対象をグループ単位で管理できます。

### Kelp Highlight
昆布系ブロックをモデルベースのオーバーレイで強調します。

### Glass Highlight
ガラス系ブロックを見分けやすくする描画補助です。既存のベースモデルを維持したままChiseTweaks側のオーバーレイを追加します。

### Lava Source Highlight
読み込み済みチャンクだけを対象に溶岩源を走査し、保持型レンダリングで表示します。チャンクを強制ロードしません。

### Ancient Debris Analyzer
古代の残骸を独立した範囲・最大表示数で解析し、壁越し確認用の保持型描画を行います。

### Chise Texture
`chise_texture_pack_mc26.1.2` をChiseTweaks内の **Fabric built-in resource pack** として統合した視覚機能です。

置き換えるのは次の4テクスチャだけです。

- White Concrete
- Chest
- Double Chest Left
- Double Chest Right

外部のresource-pack ZIPを別途Prism Launcherへ追加する必要はありません。初期状態では有効ですが、Minecraftの **Options → Resource Packs** から通常のリソースパックと同じように無効化・再有効化できます。

ゲーム内テクスチャ4枚はアップロード元と同一バイトを維持しています。`pack.png`だけはMOD JAR容量を抑えるため64x64へ縮小しています。

## 導入方法

### Prism Launcher

1. Minecraft `26.1.2` / Fabric のインスタンスを用意します。
2. Java `25` を使用するようPrism Launcher側で設定します。
3. Fabric APIを導入します。
4. GitHub Releasesから最新の `chise-tweaks-<version>.jar` を取得します。
5. Prism Launcherの **Mods** に、そのJARを1個だけ追加します。
6. Minecraftを起動します。

ChiseTweaksはクライアント専用なので、Minecraftサーバーの`mods`フォルダへ入れる必要はありません。

## 設定

Mod Menuを導入している場合は、Mod MenuからChiseTweaksの設定画面を開けます。Mod Menu自体は必須依存ではありません。

ローカル設定はMinecraftインスタンスの`config`ディレクトリに保存されます。

- `chisetweaks.json`
- `chisetweaks-visual.json`

設定JSONは読み込み時に検証・サニタイズされ、不正または安全でない内容はそのまま適用しません。設定画面では変更内容をメモリ上で扱い、Apply / Doneを永続化境界として保存します。

Chise TextureだけはMinecraft標準のResource Packs管理を利用するため、ChiseTweaksのJSON設定には重複状態を保存しません。

## 安全性・障害分離

ChiseTweaksはクライアント単体で完結することを前提にしています。

- サーバーMOD不要
- カスタムPlay Protocolなし
- リモートMOD検出なし
- バックグラウンドスレッドなし
- 自動MODダウンロードなし
- 自動JAR置換なし
- 外部設定ライブラリ不要
- Mod Menuは任意
- Feature障害はquarantine方式で隔離
- Mixin障害はfail-softを基本方針とする

描画機能の一部で障害が発生した場合も、可能な限り該当機能だけを隔離し、Minecraftクライアント全体への波及を抑える設計です。

Chise TextureはvanillaテクスチャをMOD直下から常時上書きせず、独立したbuilt-in resource packとして登録します。そのため他リソースパックとの優先順位はMinecraft標準のResource Packs画面で管理できます。

## パフォーマンス方針

大規模建築・技術施設での利用を想定し、次の方針を維持しています。

- 走査対象は原則として既に読み込まれているチャンクに限定
- Ancient Debrisの既存チャンク初期探索は1tickへ集中させず、固定上限で複数tickへ分散
- 壁越し解析描画は保持型GPUバッファを再利用
- フレームごとの不要なジオメトリ再生成を回避
- Snapshotは固定上限のdouble-bufferで共有
- runtime JARのアイコンは配布時のみ256x256へ縮小
- Chise Textureのpack iconは64x64へ軽量化
- runtime JARは400KB未満を改善目標、440KB未満をCI上の回帰上限として監視

400KBは配布容量の改善目標であり、FPS向上を直接保証する値ではありません。機能・安全性・デバッグ性を削って容量だけを小さくする方針は採用していません。

## 品質保証

CIではJava 25環境で以下を実行します。

- コンパイル / JUnit
- JaCoCo line coverage
- PIT mutation testing
- Repository audit
- Production source usage audit
- Artifact audit
- Visual asset audit
- Release residue audit
- client-only metadata検証
- runtime JAR容量検証
- built-in Chise Textureの登録方式・PNG寸法・元テクスチャSHA-256検証

Releaseは`main`のCIが成功した場合のみ自動実行され、CIで検証したものと同じcommit SHAを対象に再ビルド・再監査します。

GitHub ReleaseへChiseTweaksがアップロードするMOD成果物は **runtime `.jar` 1個だけ**です。GitHubが自動生成するSource code ZIP / tar.gzはGitHub側の標準表示です。

## バージョニング

形式:

```text
MAJOR.MINOR.PATCH+mc<MinecraftVersion>
```

例:

```text
0.9.0+mc26.1.2
```

- `PATCH`: バグ修正・互換性を壊さない改善
- `MINOR`: 互換性を維持した機能追加
- `MAJOR`: 互換性を壊す変更

同じversion/tagのReleaseは再生成しません。新しい正式JARを公開する場合はSemVerを更新します。

## Releaseから使うファイル

Minecraftへ導入するのは次のJARだけです。

```text
chise-tweaks-0.9.0+mc26.1.2.jar
```

Release workflowは公開後に、asset数、ファイル名、SHA-256、`fabric.mod.json`、Minecraft/Fabric/Java条件、client-only条件を再検証します。

## ライセンス

このリポジトリには `MIT` / `Apache-2.0` のライセンス情報とNOTICEが含まれます。再配布・公開範囲を変更する場合は、リポジトリ内のLICENSE / NOTICEを確認してください。
