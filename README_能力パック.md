# 能力パックの作り方

外部能力パックは Java コードを変更せずに能力の定義を追加する仕組みです。MOD は起動時に `config/abilitysgorwmod/ability_packs/*.json` を読み込みます。ゲーム起動中の再読み込みには対応しません。

## フォルダ構成

初回起動時に次の場所が作られ、`template.json` と `sample.json` が生成されます。

```
.minecraft/config/abilitysgorwmod/ability_packs/
  template.json
  sample.json
  README_最初にお読みください.txt
  my_pack.json       <- 作成したファイル
```

`template.json` はコピーして編集します。定義ファイルは UTF-8 の JSON で、ルートに `abilities` 配列を置きます。テンプレートは定義として読み込まれません。

## 能力の項目

| 項目 | 必須 | 内容 |
| --- | --- | --- |
| `id` | 必須 | `namespace:path` 形式。小文字英数字、`_`、`-`、`.`、`/`。既存能力と全パックで一意にする |
| `name` | 必須 | 表示名 |
| `description` | 必須 | 説明 |
| `rarity` | 任意 | `COMMON` (既定)、`RARE`、`EPIC`、`LEGENDARY` |
| `obtainable` | 任意 | 抽選で獲得できるか。既定 `true` |
| `toggleable` | 任意 | 能力画面から有効・無効を切り替えられるか。既定 `true` |
| `effect` | 任意 | 現在サポートされる効果と設定。省略時は定義のみ追加 |

IDは例として `my_pack:swift` のように、パック固有のnamespaceを使ってください。組み込み能力のID（`speed_boost` 等）や他パックと重複すると、その定義は読み込まれません。

## 効果タイプ

現在の効果は能力が有効な間に効果ポーションを維持するタイプです。効果名は大文字小文字を区別します。

| `effect.type` | ゲーム内効果 | 必須/任意パラメータ |
| --- | --- | --- |
| `speed` | 移動速度上昇 | `type` 必須、`amplifier` 任意 (0～10、既定0)、`duration` 任意 (1～1200 tick、既定100) |
| `haste` | 採掘速度上昇 | 同上 |
| `strength` | 攻撃力上昇 | 同上 |
| `jump_boost` | 跳躍力上昇 | 同上 |
| `night_vision` | 暗視 | 同上 |
| `fire_resistance` | 火炎耐性 | 同上 |
| `water_breathing` | 水中呼吸 | 同上 |
| `slow_falling` | 低速落下 | 同上 |
| `regeneration` | 再生 | 同上 |
| `resistance` | 耐性 | 同上 |
| `invisibility` | 透明化 | 同上 |
| `luck` | 幸運 | 同上 |

例:

```json
{
  "abilities": [
    {
      "id": "my_pack:swift",
      "name": "疾風",
      "description": "有効な間、移動速度上昇を得る。",
      "rarity": "RARE",
      "obtainable": true,
      "toggleable": true,
      "effect": { "type": "speed", "amplifier": 0, "duration": 100 }
    }
  ]
}
```

JSONから任意のJavaコードは実行できません。`triggers` で被ダメージ・攻撃・撃破などのイベント、確率・クールダウン・体力・対象の条件、状態異常・回復・ダメージ・アイテム付与などの動作を組み合わせられます。専用処理はMOD側の実装が必要です。未知の項目や範囲外パラメータは能力定義のエラーとして記録します。

## 読み込み・トラブルシューティング

- MODを起動する前に `.minecraft/config/abilitysgorwmod/ability_packs/` へ JSON を置きます。
- 起動ログで `Loaded external ability` を検索します。
- `AbilityPacks` ログにファイル名、ID、設定エラーが出ていないか確認します。
- 読み込まれない場合は JSON の括弧・カンマ、UTF-8、`abilities` 配列、IDの重複、レアリティの綴り、効果名と数値範囲を確認してください。
- `template.json` は読み込み対象外です。独自ファイル名 (例 `my_pack.json`) を使ってください。
- `toggleable: true` の能力は能力画面でONにしてください。`toggleable: false` は所持中ずっと有効です。パック定義で `obtainable` を `false` にすると獲得抽選から除外されます。
- マルチプレイではサーバー側にも同じパックを置いてください。能力データはサーバーで管理されます。
## ダンシングとアニメーション配布

`dancing` は組み込みのEpic能力です。能力一覧でONにすると再生でき、OFF中はサーバーが再生要求を拒否します。操作設定から「待機アニメーション再生」「攻撃アニメーション再生」「アニメーション停止」にキーを割り当てられます。再生要求はサーバーが能力所持を確認してから同じディメンションで再生中のプレイヤーを追跡するクライアントへ送ります。アニメーションデータは既存の独自キーフレームJSONを使います。

設定GUIは `M` キー、または能力一覧 (`K`) 内の「ダンシング設定」から開けます。「ファイルを開く」でJSONを選び、登録名（既定では選んだファイル名）とJSON内のクリップ名を入力して「登録」を押してください。IDは登録名から自動生成されます。登録ごとのキー設定は不要です。`N` キーまたは設定GUIの「再生リスト」からアニメーションを選んで再生します。再生にはダンシングをONにしてください。設定は `config/abilitysgorwmod/dancing_animations.json` に保存され、インポート・エクスポートにも対応します。取り込み元は既存の独自キーフレームJSONで、`animations` 内のクリップと `loop` 設定をそのまま使います。

マルチプレイで同期するのは再生プレイヤーID、リソース識別子、クリップ名、停止指示です。アニメーション本体は配布しません。クライアントで計算したSHA-256を同期命令と一緒に送り、受信側で同じハッシュか検証します。異なるデータや欠落を検出したクライアントは再生しません。取り込んだファイルは各クライアントの `config/abilitysgorwmod/animations/custom/<ID>.json` にも置いてください。全クライアントに同じデータが必要です。データがないクライアントはログに失敗を記録し、そのアニメーションを再生できません。

## Java API を使う他MOD開発者へ

能力登録、カスタムレアリティ、プレイヤーデータ、能力イベントの使い方は、ソース内の `docs/ABILITY_API_他MOD開発者向け.md` を参照してください。
