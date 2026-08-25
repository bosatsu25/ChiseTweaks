# ChiseTweaks

> 大規模建築・技術施設の見落としを減らす、Fabric クライアント専用 Build Inspector / Visual QA MOD。

ChiseTweaks は、視認性向上、Block / Entity Filter、Crosshair Inspector、Placement Preview / Actual Comparison、Lava / Ancient Debris Analyzer を1つにまとめます。サーバー導入や独自通信、自動操作は不要です。

## 必要環境

| 項目 | 対応 |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 動作側 | Client only |
| Mod Menu | 任意 |
| Sodium | 任意・推奨 |

## 導入

Prism Launcher の Mods に、CI / GitHub Release から取得した runtime JAR を1個だけ追加します。

```text
chise-tweaks-<version>.jar
```

ChiseTweaks はクライアント専用です。サーバー側の `mods` へ入れる必要はありません。

## 現在の機能

主要な runtime feature は11個で、すべて初期OFFです。機能は明示的な仕様がない限り相互排他にせず、全機能同時ONを前提にします。

| 系統 | 機能 | 概要 |
| --- | --- | --- |
| Highlight | Ore Highlights | 鉱石・古代の残骸・黒曜石系を強調 |
| Highlight | Nether Highlight | ネザー建築素材を補助表示 |
| Highlight | Fine Line Highlight | Tripwire / Tripwire Hookなど細線対象を強調 |
| Highlight | Hidden Block Highlight | Powder Snow / Blue Ice / Dead Coral / Sculk Catalystなどを強調 |
| Highlight | Glass Highlight | Glass / Glass Paneを形状別に強調 |
| Highlight | Kelp Highlight | Kelp / Kelp Plantへ視認性オーバーレイを追加 |
| Filter | Block Filter | Allow / Hide listを通常Block・BlockEntity・Chise overlayへ適用 |
| Filter | Entity Filter | Entity IDのAllow / Hide listでローカル描画を制御 |
| Analyzer | Lava Analyzer | loaded chunksの近距離で溶岩源だけを表示 |
| Analyzer | Ancient Debris Analyzer | Netherのloaded chunksから古代の残骸を検出 |
| Visibility | Low Fire | 一人称の炎オーバーレイを低く表示 |

Built-in Resource Packとして `Bright Chest` と `Bright Concrete` も提供します。これらはruntime feature数には含めません。

## Inspector

Crosshair InspectorはMinecraftがすでに保持している照準結果を読み取り、次を表示します。

- Block / Entity ID
- Orientation / Shape / Connection / Interaction / Fluidへ意味分類したprivacy-safeなBlockState properties
- 未知のmodded propertyを保持するOther / Properties fallback
- Filter判定とmatched rule
- Responsible Feature
- VISIBLE / THROUGH_WALLなどの描画モード

NBT、看板本文、本、chat、inventory、container内容、UUIDは取得しません。追加raycast、packet送信、world変更も行いません。

### Placement Preview / Actual Comparison

配置前のBlockStateはvanilla `getStateForPlacement`を利用して最大1件だけ予測します。

- Trapdoor: Facing / Half / Open / Powered / Waterlogged
- Log / Wood / Stem / Hyphae / Froglight: Axis
- Slab: Type / Waterlogged
- Stairs: Facing / Half / Shape / Waterlogged
- Glazed Terracotta: Facing
- Fence Gate: Facing / Open / Powered / In Wall
- Grindstone: Face / Facing
- Beehive / Bee Nest: Facing prediction; Honey Level is shown only from observed Actual state
- Campfire: Facing / Lit / Signal Fire / Waterlogged

通常のvanilla配置後はその1座標だけを短時間観測し、PredictedとActualを `MATCH` または `ADJUSTED` として関連付け、変化したpropertyを表示します。別blockが置かれた場合は比較せず、timeout、disconnect、dimension変更でpending stateを破棄します。

packet送信、world変更、click / key注入、追加raycast / scan、設定保存、配置履歴は行いません。Stairsなど複雑block、3D ghost、offhandは今後のIssueで扱います。

## Filter precedence

Block FilterのHIDEを最優先にします。

```text
Block Filter = HIDE
  -> Vanilla Block rendering: hidden
  -> BlockEntity rendering: hidden
  -> Bright Chest: hidden
  -> Highlight / Overlay: hidden
```

## Analyzerの負荷境界

Analyzerは既にロード済みのクライアントchunkだけを扱い、未ロードchunkを強制ロードしません。

Ancient Debris Analyzerはbootstrap / validationをtickへ分散し、追跡数・marker数にも上限を持ちます。Lava Analyzerも水平・垂直範囲と更新間隔をboundedに保ちます。

## 設定

設定ファイル:

- `config/chisetweaks.json`
- `config/chisetweaks-visual.json`

managed settingsはApply / Doneで永続化します。Bright Chest / Bright Concreteの選択状態はMinecraft標準Resource Pack状態を正とします。

## 開発・QA

開発環境、CI、Versioning、Release、JAR size、Prism acceptance、performance、securityの現在仕様は [`DEVELOPMENT.md`](DEVELOPMENT.md) を正本とします。

今後の機能計画と作業単位はGitHub Issuesを正本とし、過去の経緯はGit / Pull Request履歴へ残します。

## 配布上の注意

公開配布に関するprovenance / attributionは `LICENSE` / `LICENSE_MIT` / `LICENSE_APACHE-2.0` / `NOTICE` の現在内容に従います。これらはRepository整理を理由に削除・統合しません。
