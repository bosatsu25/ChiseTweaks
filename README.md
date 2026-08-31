# ChiseTweaks

> **建築時の視界・可視化・確認作業を軽く補助するFabricクライアントTweaks MOD**です。

ChiseTweaksは、MinecraftやMasa系MODの既存動作を大きく置き換えず、建築時に必要な情報を**見やすくする・必要なものだけ表示する・配置前後を確認する**ための小さな改善をまとめます。

**Auto Eat / Auto Restock / Auto Move / Auto Totem / Auto Repair / Auto Drop / Auto Fill Schematic Inventory / Auto Void Trade / 自動クリック / 自動建築は実装しません。**  
プレイヤーの操作を代行するのではなく、**Visual / Builder / Workflow Tweaks**へ範囲を絞ります。

Repository version: **`0.15.0+mc26.1.2`**  
このREADMEは`main`へ提案中の現在仕様を含む場合があり、配布済みReleaseより先行することがあります。

---

## 必要環境

| 項目 | 必要条件 |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.3` 以上 |
| Fabric API | `0.155.2+26.1.2` 以上 |
| Java | `25` 以上 |
| 導入先 | クライアントのみ |
| Mod Menu | 任意 |

配布JARは **`chise-tweaks-<version>.jar`** です。サーバー側へChiseTweaksを導入する必要はありません。

### 導入

1. Minecraft 26.1.2 / Fabric環境を用意します。
2. Fabric APIを導入します。
3. `chise-tweaks-<version>.jar` を対象インスタンスの`mods`へ入れます。
4. Mod Menuがある場合は **Mods → ChiseTweaks → Config** から設定できます。

Prism Launcherでは、インスタンス編集画面の **Mods → ファイルを追加** からJARを追加できます。

---

# ChiseTweaksの考え方

内部には現在、**16個のON/OFF可能なruntime機能**があります。  
ただしユーザーに16個の独立した製品を覚えてもらう設計にはしません。Tweaks MODとして、機能を次の**6つのグループ**へ整理します。

| グループ | 目的 |
| --- | --- |
| **Visual Tweaks** | 一人称表示や暗所表示を少し見やすくする |
| **Builder Highlights** | 建築対象・建材・見つけにくい対象を強調する |
| **Technical Visualization** | 範囲・接続・技術設備の関係を可視化する |
| **Scene Filter** | Block / Entityの表示対象を絞る |
| **Builder Assist** | BlockState・配置・パターン・設計図・履歴を確認する |
| **Integrations** | Masa系MODやrendererとの連携を安全に補助する |

初期状態では **Bright Chest / Bright ConcreteだけON**、それ以外の14機能はOFFです。必要なTweaksだけONにしてください。

---

## 1. Visual Tweaks

日常的な視界改善です。world scanを主目的にせず、既存描画へ局所的に作用します。

| 機能 | 内容 | 初期値 |
| --- | --- | --- |
| **Low Fire** | 一人称の炎をLarge / Medium / Smallで低く・小さくする | OFF |
| **Handheld Size** | 一人称の手持ちItemを種類別に小さくする | OFF |
| **Bright Chest** | Chestを暗所でも確認しやすくする | ON |
| **Bright Concrete** | White Concreteを暗所でも確認しやすくする | ON |

Handheld Sizeは現在のResource Packを置き換えません。初期倍率は **Block 70% / Item 60% / Weapons & Tools 75%**、Shieldは95%です。GUI・三人称・world itemには適用しません。

**Bright系はResource Pack切替を持ちません。**  
通常のMinecraft描画経路で視認性を補助し、Bright切替のためのResource Pack reloadを要求しません。また、実装上も **White Concrete spriteをChestへ貼らない** ことを品質契約として扱います。

---

## 2. Builder Highlights

「このブロックを建築中だけ見つけやすくしたい」をまとめるグループです。

| 対象 | 内容 |
| --- | --- |
| **Ore Highlights** | 鉱石、古代の残骸、黒曜石系を強調 |
| **Nether Highlight** | Nether建材を色分けして確認しやすくする |
| **Glass Highlight** | Glass / Glass Paneの形を確認しやすくする |
| **Kelp Highlight** | Kelp / Kelp Plantを水中で確認しやすくする |
| **Lava Source Highlight** | 読み込み済み近傍のLava Sourceを壁越しmarkerで可視化 |
| **Hidden Material Highlight** | Powder Snow / Blue Ice / Dead Coral / Sculk Catalystを可視化 |

### Occluded Highlightについて

Lava Source Highlight / Hidden Material Highlightは、以前の独立した「Analyzer」という扱いではなく、**Builder Highlightsの壁越し可視化target**として扱います。

安全・性能上の境界は維持します。

- already-loaded chunksだけを見る
- 未ロードchunkを強制読み込みしない
- ChiseTweaks独自のscan packetを送らない
- server Anti-X-Rayを迂回しない
- radius / vertical radius / interval / marker数をboundedにする
- 初期OFF

サーバールールでthrough-wall表示がX-Ray扱いになる場合は使用しないでください。

---

## 3. Technical Visualization

建築設備の**範囲・接続・関係**を見えるようにするグループです。

| 機能 | 内容 |
| --- | --- |
| **Fine Line Highlight** | Tripwire / Tripwire Hookなど細いtechnical blockを可視化 |
| **Beacon Range** | Beaconの有効範囲を表示 |
| **Lightning Rod Range** | 避雷針の有効範囲を表示 |
| **Villager Job Site Links** | Villager → Job Siteの既知の関係を線で表示 |

### Villager Job Site Links

ChiseTweaksは、MinecraftがVillagerの`JOB_SITE` memoryとして**すでに把握している関係だけ**を表示します。

以前のfallback workstation推測は廃止します。つまり、Job Siteが分からないVillagerの周囲をBlock scanして「たぶんこの職業ブロック」と推測しません。

これにより、

- 誤推測を減らす
- workstation block-volume scanを削除する
- runtime workと状態管理を減らす
- 「解析」ではなく「既知情報の可視化」に責務を限定する

というTweaks寄りの設計にします。

---

## 4. Scene Filter

ワールドを変更せず、**自分の画面上で何を描画するか**を絞ります。

| 機能 | 内容 |
| --- | --- |
| **Block Filter** | Block IDのAllow / Hideルールで表示対象を絞る |
| **Entity Filter** | Entity IDのAllow / Hideルールで表示対象を絞る |

大量のBlock / Entityを消去する機能ではありません。表示ルールだけを切り替えます。

---

## 5. Builder Assist

旧Inspectorの能力は捨てず、**建築作業に直接役立つものだけをBuilder Assistとして残します。**

| 機能 | 内容 |
| --- | --- |
| **Block Info** | 見ているBlockのID / BlockState / 向きなどを確認 |
| **Placement Assist** | 配置前の予測と配置後の実際のstateを確認 |
| **Pattern Check** | 基準Blockと周囲の同種Blockのstate差分を確認 |
| **Litematica Placement Assist** | Expectedと配置候補を比較 |
| **Interaction History** | 直近の配置・破壊・使用・interactionをメモリ内で確認 |

Placement Assistは自動配置でも配置禁止でもありません。Minecraftの操作自体はプレイヤーが行います。

通常UIからは、ユーザー価値の低い開発者向けdiagnostic情報を外します。

- Filter Decision
- Matched Rule
- Responsible Feature
- Render Mode

これらを常用UIへ出すためだけの製品複雑性は持たせません。

---

## 6. Integrations

Masa ecosystemとrenderer compatibilityはoptionalです。対象MODが無ければ、その連携だけno-opになります。

| 対象 | ChiseTweaks側の補助 |
| --- | --- |
| **Litematica** | Pick Redirect / Placement Assist |
| **Tweakeroo** | Tool Switch Guard / Persistent Gamma Override復元 |
| **TweakerMore** | Auto Pick Guard / Material List Refresh |
| **Syncmatica** | Remove Disable / Require Shift To Remove |
| **MaLiLib / Masa系全体** | Japanese UI / Masa Guide |
| **Nvidium** | World Border / far-coordinate rendering compatibility guard |

`Auto Pick Guard` や `Tool Switch Guard`は、ChiseTweaks自身がAuto Pick / Tool Switchを実行する機能ではありません。外部MODの既存動作へAllow / Denyルールを追加します。

---

# ChiseTweaksがやらないこと

製品スコープは**client-only / no automation ownership**です。

- 自動飲食
- 自動補充
- 自動移動
- 自動Totem
- 自動Repair
- 自動Drop
- 自動Firework
- 自動Inventory操作
- Auto Fill Schematic Inventory
- Auto Void Trade
- 自動click / keyboard injection
- 自動建築 / 自動配置
- custom play protocol
- forced chunk loading

「何でも入りUtility MOD」ではなく、**建築者向けのVisual / Workflow Tweaks**として品質を上げます。

---

# 設定と互換性

既存ユーザーの設定を壊さないため、製品グループを変更しても内部のfeature IDや既存config keyは安易にrenameしません。

主な設定ファイル:

| ファイル | 内容 |
| --- | --- |
| `chisetweaks.json` | 基本Highlight / Filter |
| `chisetweaks-visual.json` | Visual / Technical / Builder Assist周辺 |
| `chisetweaks-integrations.json` | Masa ecosystem連携 |
| `chisetweaks-compatibility.json` | renderer compatibility |

壊れたJSON、型不一致、古いschemaは安全側へfallbackする設計です。設定名や型を変更する場合はmigration fixtureと回帰テストを追加してから変更します。

---

# 品質保証

ChiseTweaksは、機能数よりも**回帰しにくさ・軽量性・配布物の再現性**を重視します。

現在の品質ゲート:

| 項目 | 閾値 |
| --- | ---: |
| JaCoCo line coverage | 96% |
| PIT coverage | 96% |
| Mutation score | 96% |
| Test strength | 96% |
| runtime JAR目標 | 296,960 bytes / 290 KiB |
| runtime JAR stretch | 256,000 bytes / 250 KiB |
| runtime JAR hard ceiling | 446,814 bytes |

Performanceは推測値で合格判定しません。同一Prism環境でstartup / p50 / p95 / p99 frametime / heap / allocation / render-thread CPU / average FPSを比較します。

容量削減ではSourceFile / LineNumberを消したり、obfuscationだけで数字を作ったりせず、**重複責務・不要scan・不要state・不要UIを減らす**ことを優先します。

---

# 開発者向け

実装境界、QA、CI、Release、Performance契約は [`DEVELOPMENT.md`](DEVELOPMENT.md) を参照してください。

代表コマンド:

```bash
./gradlew --stacktrace ciGate
xvfb-run -a ./gradlew --stacktrace runClientGameTest
```

Releaseでは、可能な限りCIで検証したruntime JARそのものを昇格させ、tree SHA / runtime SHA-256 / provenanceの一致を確認します。

---

## 方針を一文で

> **建築者が必要な情報を、軽く・見やすく・安全に出す。操作そのものは奪わない。**
