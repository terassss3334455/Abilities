# AbilitysgorwMOD 引き継ぎメモ

更新日: 2026-10-25 (日本時間)

## プロジェクト

- メインMOD: C:\bluearchiveMODFile\teramodGuard
  - Minecraft Forge 1.20.1 / Forge 47.4.10
  - MOD ID abilitysgorwmod
  - 現行バージョン 1.0.8
  - 通信プロトコル 3。新旧JARを混在させず、サーバーと全クライアントを同じ版にする。
- Curiosアドオン: C:\bluearchiveMODFile\curiosAbilityAddon
  - MOD ID curiosabilityaddon
  - 現行バージョン 1.0.0
  - Curios Forge 1.20.1 が必須。開発時は curios-forge:5.14.1+1.20.1 を参照。
  - アドオンの mods.toml はメインMOD [1.0.5,)、Curios [5.0,) を要求する。現在のメインMOD 1.0.8 は範囲内。
  - MOD IDが異なり、依存関係も一方向（アドオン → メインMOD）なので同時導入できる。
  - Curiosの ability_enhancer スロットは size: 3。プレイヤーに3枠付く。

## 最新ビルド成果物

- メインMOD: C:\bluearchiveMODFile\teramodGuard\build\libs\abilitysgorwmod-1.0.8.jar
- Curiosアドオン: C:\bluearchiveMODFile\curiosAbilityAddon\build\libs\curiosAbilityAddon-1.0.0.jar
- 導入時は上記2つと、Minecraft 1.20.1対応のCurios Forge JARを mods に置く。マルチではサーバーと接続する全クライアントに同じMOD一式を入れる。

## 主な実装

### メインMOD

- 能力登録・プレイヤーデータ・レアリティ・確率補正・能力イベント用のAPIがある。
  - api/AbilityApi.java
  - api/AbilityRarityApi.java
  - api/AbilityChanceCatalog.java
  - api/event/AbilityApiEvents.java
- 能力一覧画面、能力ON/OFF、ダンシング設定・アニメーション選択画面、プレイヤー用無限収納を実装。
  - キー初期値: K=能力一覧、M=ダンシング設定、N=アニメーション選択、H=境界渡り、B=無限収納。
  - カスタムアニメーションはJSON設定・ファイル選択を行う。再生同期は同じリソースを持つクライアントで表示する想定。
- インベントリに「能力効果」ボタンを追加。確率補正タブは基礎値・補正後確率を表示し、強化効果タブは現在有効な状態効果を表示する。
- ハード難易度でスポーンする敵対モブにランダムな戦闘能力を保存。通常は1個、ボスバー持ちは2個。ボスバー判定は ServerBossEvent フィールドも確認する。
- プレイヤー用「突進」: Rキー（キー設定で変更可）。能力を取得してONにすると視点方向へダッシュし、進路上のモブに現在の攻撃力分のダメージ。クールダウン5秒。
- 「怨念」: 近接攻撃が命中すると、毒・鈍足・弱体化などから状態異常を1つランダム付与。
- 突進・境界渡り・瞬間転位・JSON能力パックのクールダウンを、防具ゲージの上のHUDバーで表示。
- 効果音・パーティクル、能力ステータス表示、ダッシュ中の判定、能力パックのイベント/クールダウン連携を含む。

### Curiosアドオン

- 6種類の一枠装備アイテム（確率レンズ、戦闘・収穫・宝物・生命力・俊敏のお守り）を登録。
- 確率レンズは確率を一律+30ポイント、カテゴリ別お守りは対象能力に+20または+25ポイント補正。最終値はメインAPIの確率イベント経由。
- 一部お守りは装備中に攻撃力・採掘速度・幸運・再生・耐性・移動速度・跳躍などの状態効果を付与。
- エンドシティと砂漠ピラミッドのチェストにお守りを追加。
- アドオン独自のテクスチャ・モデル・日本語/英語名を同梱。

## ビルドと確認

メインMOD:

~~~powershell
cd C:\bluearchiveMODFile\teramodGuard
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat reobfJar
~~~

アドオン:

~~~powershell
cd C:\bluearchiveMODFile\curiosAbilityAddon
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat build
~~~

2026-10-25にメイン reobfJar とアドオン build は成功。日本語/英語lang JSONも解析済み。Minecraftを起動した実機テストは未実施。

## 次の確認事項

1. Forge 1.20.1環境でメインMOD、Curios、アドオンを同時に起動する。
2. 能力一覧で「突進」「怨念」をONにし、Rキーの移動・命中ダメージ・5秒ゲージと怨念の状態異常を確認する。
3. ハードで通常敵が1能力、ボスバー持ちが2能力を得ること、敵の突進が敵対対象へ発動することを確認する。
4. Curiosの3枠、各お守りの装備効果、確率補正、チェスト戦利品を確認する。
5. death_explosion も敵の抽選候補に入っており、死亡時にMOB扱いの威力6の爆発を起こす。地形破壊を避けたい場合は候補から外すか爆発設定を調整する。

## 重要な実装ファイル

- メイン能力処理: src/main/java/net/coms/abilitysgorwmod/ability/AbilityEffectHandler.java
- 突進: src/main/java/net/coms/abilitysgorwmod/ability/ChargeAbility.java
- ハード用モブ能力データ: src/main/java/net/coms/abilitysgorwmod/ability/HardModeCombatAbilityData.java
- HUD: src/main/java/net/coms/abilitysgorwmod/client/AbilityCooldownHudHandler.java
- アドオン登録・補正: src/main/java/net/coms/curiosabilityaddon/
- Curios 3枠: src/main/resources/data/curiosabilityaddon/curios/slots/ability_enhancer.json