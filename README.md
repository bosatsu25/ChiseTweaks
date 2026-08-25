# ChiseTweaks

> Minecraftで「見えにくい」「置き方が合っているか分からない」「大きな建築の確認がつらい」を減らす、建築向けのFabricクライアントMODです。

ChiseTweaksは、**ブロックを見つけやすくする・不要なものを画面から隠す・置いたブロックの向きを確認する・近くの危険物や対象ブロックを探しやすくする**ための機能を1つにまとめています。

自動で建築したり、サーバーのワールドを書き換えたりするMODではありません。基本的には「自分の画面で見やすくする」「自分の画面で確認する」ためのMODです。

## まずはここだけ読めば使えます

1. Minecraft `26.1.2` のFabric環境を用意します。
2. `chise-tweaks-<version>.jar` をMinecraftの `mods` フォルダーへ入れます。
3. 初心者の方は **Mod Menu** も入れておくのがおすすめです。Mod Menuの一覧からChiseTweaksの設定画面を開けます。
4. ChiseTweaksの通常機能は**初期状態ではOFF**です。必要な機能だけONにしてください。
5. 設定を変更すると画面下のボタンが「設定を適用」に変わります。「設定を適用」または「Done」で保存できます。

Prism Launcherを使う場合は、対象インスタンスを編集して `Mods` からChiseTweaksのJARを追加すればOKです。

> **マルチプレイで使う場合**  
> Lava AnalyzerやAncient Debris Analyzerは壁越し表示を行います。クライアント専用MODでも、サーバーによっては使用を禁止している場合があります。参加しているサーバーのルールを確認してから使用してください。

## 必要環境

| 項目 | 対応 |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 導入する場所 | クライアントのみ |
| Mod Menu | 任意・初心者には推奨 |
| Sodium | 任意・推奨 |

ChiseTweaksは**サーバー側の `mods` フォルダーへ入れる必要はありません**。

## 設定画面の見方

設定画面は5つのタブに分かれています。

| タブ | 何をする場所？ |
| --- | --- |
| **Highlight** | 鉱石、ガラス、昆布、糸など「見つけにくいブロック」を見やすくします。 |
| **Filter** | 見たいブロック・エンティティだけ残したり、邪魔なものを隠したりします。 |
| **Inspector** | 今見ているブロックの情報、置く向き、置いた結果、周囲との違いを確認します。 |
| **Analyzer** | 近くの溶岩源や古代の残骸を、壁越しのマーカーで確認します。 |
| **Visibility** | 燃えている時の炎を低くしたり、チェストや白色コンクリートを見やすくしたりします。 |

迷った場合は、まず **Highlight** と **Visibility** だけ触れば十分です。FilterとInspectorは「建築の確認をもっと細かくしたい」と感じてから使うと分かりやすいです。

## 目的から機能を選ぶ

| こんな時 | 使う機能 |
| --- | --- |
| 鉱石を見落としたくない | **Ore Highlights** |
| ガラスの境目が分かりにくい | **Glass Highlight** |
| トリップワイヤーや糸が見えにくい | **Fine Line Highlight** |
| 粉雪など見落としやすいブロックを確認したい | **Hidden Block Highlight** |
| 昆布を水中で見つけやすくしたい | **Kelp Highlight** |
| ネザー建材を見分けやすくしたい | **Nether Highlight** |
| 近くの溶岩源を壁越しに確認したい | **Lava Analyzer** |
| ネザーで読み込み済み範囲の古代の残骸を確認したい | **Ancient Debris Analyzer** |
| 燃えている時に画面中央を見やすくしたい | **Low Fire** |
| 特定のブロックだけ表示したい・隠したい | **Block Filter** |
| 特定のMobやエンティティだけ表示したい・隠したい | **Entity Filter** |
| ブロックの向きや状態を確認したい | **Inspector** |
| 同じブロックを大量に置いた時の向き間違いを探したい | **Pattern Consistency** |

## Highlight — 見つけにくいものを見やすくする

### Ore Highlights

鉱石、古代の残骸、黒曜石系などへ、元の見た目を残したまま強調表示を重ねます。

これは**壁越しに鉱石を探す機能ではありません**。見えている対象を見落としにくくするためのHighlightです。壁越しに古代の残骸を確認する機能は、別の **Ancient Debris Analyzer** です。

### Glass Highlight

透明ガラス、遮光ガラス、色付きガラス、板ガラスを見分けやすくします。元のガラスの色を残したまま、形が分かるマーカーを重ねます。

### Fine Line Highlight

糸やトリップワイヤーフックのような細くて見落としやすい対象を、近距離で確認しやすくします。

### Hidden Block Highlight

粉雪、青氷、死んだサンゴ、スカルクカタリストなど、建築や確認作業で見落としやすい対象を強調します。

### Kelp Highlight

昆布と昆布の茎へマゼンタ×オレンジのネオン表示を重ね、水中でも形を追いやすくします。

### Nether Highlight

見えているネザー建材へ色分けした線を表示し、似た色のブロックを識別しやすくします。

## Filter — 画面から邪魔なものを減らす

### Block Filter

ブロックIDを指定して、**表示を残す / 非表示にする**ルールを作れます。

たとえば、大きな装置の確認中に「この種類のブロックだけ見たい」という使い方ができます。通常ブロックだけでなく、BlockEntityやChiseTweaksのハイライトにも同じ非表示ルールが適用されます。

**Block FilterでHIDEにした対象は、HighlightをONにしていても非表示が優先されます。**

### Entity Filter

MobやエンティティのIDを指定して、表示を残す / 非表示にするルールを作れます。

Filterは設定を間違えると「ブロックやMobが消えたように見える」ため、初心者の方は必要になってから使うのがおすすめです。ワールドから実際に削除しているわけではなく、ローカルの描画だけを制御します。

## Inspector — ブロックの状態と置き方を確認する

Inspectorタブを開くと、照準を合わせているブロックやエンティティを確認できます。

ブロックなら、IDだけでなく、向き・形・接続・水没状態などをMinecraftのBlockStateから読み取り、分かりやすいグループに分けて表示します。

### Placement Preview

ブロックを置く前に「この向きで置かれそうか」を予測します。

たとえば階段なら向きや上下、原木なら軸、トラップドアなら向きや上下などを確認できます。

### Actual Comparison

実際にブロックを置いた後、その1か所だけを短時間確認して、予測どおりなら `MATCH`、Minecraft側で状態が調整された場合は `ADJUSTED` と表示します。

「置くつもりだった向き」と「実際に置かれた状態」が違っていないかを確認するための機能です。

### Pattern Consistency

同じ種類のブロックを大量に並べた時の向き間違いなどを探す機能です。

Inspectorで正しいブロックを1つ **Reference** として選ぶと、そのブロックを基準に、近くにある**同じBlock ID**だけを比較します。多数決で正解を決めるのではなく、自分がReferenceとして選んだブロックが基準です。

<details>
<summary>Placement Previewが現在対応しているブロックを見る</summary>

- Trapdoor
- Log / Wood / Stem / Hyphae
- Froglight
- Slab
- Stairs
- Glazed Terracotta
- Fence Gate
- Grindstone
- Beehive / Bee Nest
- Campfire

Beehive / Bee NestのHoney Levelは予測せず、実際に観測した状態だけを表示します。

</details>

<details>
<summary>Pattern Consistencyの現在の走査上限を見る</summary>

- Reference中心の水平範囲: 8ブロック
- 上下範囲: ±4ブロック
- 1tickで確認する最大数: 256ブロック
- 保持する不一致: 最大64件
- 再確認間隔: 20tick
- 対象: クライアントがすでに読み込んでいるチャンクのみ

Referenceはメモリ上だけに保持され、Referenceを選び直した時、ディメンションを移動した時、サーバーから切断した時に破棄されます。

</details>

## Analyzer — 壁越しに近くの対象を確認する

### Lava Analyzer

近くの**溶岩源**を壁越しの立方体マーカーで表示します。流れている溶岩ではなく、溶岩源を対象にします。

表示範囲、上下範囲、更新間隔、最大表示数を設定できます。

### Ancient Debris Analyzer

ネザーで、クライアントが**すでに読み込んでいるチャンク**から古代の残骸を探し、壁越しのマーカーで表示します。

未ロードの場所を勝手に読み込んだり、遠くのチャンクをサーバーへ要求したりはしません。検出範囲と最大マーカー数にも上限があります。

> Analyzerは「クライアント専用だからどのサーバーでも使ってよい」という意味ではありません。マルチプレイでは必ずサーバールールを確認してください。

## Visibility — 普段の画面を見やすくする

### Low Fire

自分が燃えている時に表示される一人称の炎だけを画面下側へ寄せます。

ワールド上の炎そのものや、使用しているリソースパックの炎テクスチャは変更しません。

### Bright Chest / Bright Concrete

ChiseTweaksには次のbuilt-in Resource Packが含まれています。

- **Bright Chest** — Chest / Double Chestを見分けやすくします。
- **Bright Concrete** — White Concreteを見分けやすくします。

この2つは通常の11個のruntime featureとは別扱いで、**初期状態では有効**です。Visibilityタブからそれぞれ独立して切り替えられます。

Block Filterで対象を非表示にした場合は、Bright Chestなどの見やすさ変更よりBlock FilterのHIDEが優先されます。

## ChiseTweaksがしないこと

ChiseTweaksは建築確認用のクライアントMODとして、次のような操作は行いません。

- サーバー側MODの導入を要求しません。
- ChiseTweaks独自のプレイ用パケットを送信しません。
- ブロックを自動設置・自動破壊しません。
- クリックやキー入力を自動注入しません。
- 未ロードチャンクを強制的に読み込みません。
- InspectorでBlockEntityのNBT、看板本文、本、チャット、インベントリ、コンテナ内容、UUIDを取得しません。

InspectorやAnalyzerは、Minecraftクライアントがすでに持っている情報や、すでに読み込んでいる範囲を使って動作します。

## 設定の保存

通常の設定は次のファイルへ保存されます。

```text
config/chisetweaks.json
config/chisetweaks-visual.json
```

設定を変更すると「設定を適用」ボタンが表示されます。「設定を適用」または「Done」で保存します。

Bright Chest / Bright ConcreteはMinecraft標準のResource Pack有効状態を正として管理します。

## 困った時

### ChiseTweaksが起動しない

Minecraft、Fabric Loader、Fabric API、Javaのバージョンが「必要環境」と一致しているか確認してください。特にこのREADMEの現在版は **Minecraft 26.1.2 / Java 25以上** が前提です。

### 設定画面をどこから開くのか分からない

初心者の方はMod Menuを導入し、Mod Menuの一覧からChiseTweaksを選んで設定画面を開く方法が簡単です。Mod Menu自体はChiseTweaksの必須依存ではありません。

### AnalyzerをONにしたのに何も表示されない

Analyzerには範囲と表示数の上限があります。また、未ロードチャンクは対象になりません。Ancient Debris Analyzerはネザーで使用します。

### シェーダー使用時に色や見え方が違う

描画系MODやシェーダーによって最終的な見え方が変化する場合があります。ChiseTweaksはSodiumとの併用を想定していますが、実際の描画確認は使用環境でも行ってください。

## 開発・QA向け

開発環境、CI、Versioning、Release、JAR容量、Prism実機確認、performance、securityなどの現在仕様は [`DEVELOPMENT.md`](DEVELOPMENT.md) を正本とします。

現在の実装はMinecraft `26.1.2`、Fabric Loader `0.19.3`、Java `25`を基準にしています。機能計画と作業単位はGitHub Issuesを正本とし、過去の経緯はGit / Pull Request履歴へ残します。

ChiseTweaksは、明示的な仕様がない限り各機能を相互排他にせず、**複数機能を同時にONにした状態でも動作すること**を品質条件としています。

## ライセンス・配布

公開配布に関するprovenance / attributionは `LICENSE` / `LICENSE_MIT` / `LICENSE_APACHE-2.0` / `NOTICE` の現在内容に従います。
