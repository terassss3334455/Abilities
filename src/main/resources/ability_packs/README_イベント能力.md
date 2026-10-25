# イベント型の外部能力

能力JSONに `triggers` を追加すると、能力がONの間にイベントに反応する処理を設定できます。JSONから任意のJavaコードを実行する機能ではなく、許可されたイベントとアクションを組み合わせます。

## 発動イベント

- `on_hurt`: 能力を持つプレイヤーがダメージを受けた時
- `on_attack`: 能力を持つプレイヤーが攻撃を当てた時
- `on_kill`: 能力を持つプレイヤーがエンティティを倒した時
- `on_tick`: 定期処理。`cooldown` 省略時は20 tick（約1秒）ごと
- `on_jump`: ジャンプした時
- `on_item_use`: アイテムの使用を完了した時
- `on_pickup`: アイテムを拾った時
- `on_block_break`: ブロックを壊した時

## 共通設定

- `chance`: 発動確率。0～1、省略時1
- `cooldown`: 再発動までのtick数。0～72000、省略時は通常イベントで0
- `item`: `on_item_use` または `on_pickup` 用のアイテムIDフィルター。例: `minecraft:golden_apple`
- `damage_type`: `on_hurt` / `on_attack` 用のダメージ種別フィルター。例: `minecraft:fall`, `minecraft:player_attack`
- `min_damage` / `max_damage`: `on_hurt` / `on_attack` のダメージ量の範囲（0～1000）
- `target_entity`: `on_attack` / `on_kill` の対象エンティティID。例: `minecraft:zombie`
- `health_below` / `health_above`: 発動するプレイヤー自身の体力条件（HP）。0～2048
- `actions`: 1～16個のアクション
- トリガーは1能力につき最大16個

条件はすべて同時に満たした時に発動します。`on_tick` は `cooldown` を省略すると20 tick（約1秒）ごとです。

## アクション

### 効果

```json
{"type":"effect","target":"self","effect":"minecraft:speed","amplifier":1,"duration":60}
```
`target` は `self`（能力を持つ人）、`attacker`（イベントを起こした攻撃者）、`target`（攻撃/撃破の対象）です。`amplifier` は0～10、`duration` は1～1200 tickです。

### 状態異常の解除・着火

```json
{"type":"remove_effect","target":"self","effect":"minecraft:poison"}
{"type":"set_fire","target":"target","seconds":3}
```
`remove_effect` は対象から指定した状態異常を取り除きます。`set_fire` は対象を指定秒数燃やします（1～300秒、省略時5秒）。

### 回復・ダメージ

```json
{"type":"heal","target":"self","amount":2}
{"type":"damage","target":"attacker","amount":3}
```
`amount` は0より大きく100以下です。追加ダメージは魔法ダメージとして扱います。

### 被ダメージ無効

```json
{"type":"cancel_damage"}
```
`on_hurt` / `on_attack` のみで使えます。現在のダメージイベントをキャンセルします（`on_hurt` なら自分へのダメージ、`on_attack` なら自分の攻撃）。

### アイテム付与・満腹回復

```json
{"type":"give_item","item":"minecraft:golden_apple","count":1}
{"type":"feed","food":4,"saturation":2}
```
`give_item` は能力を持つプレイヤーに付与します。`food` は0～20、`saturation` は0～20です。

## 例: ゾンビを攻撃した時、体力が半分未満なら追加で毒

```json
{
  "event": "on_attack",
  "target_entity": "minecraft:zombie",
  "health_below": 10,
  "cooldown": 40,
  "actions": [
    {"type": "effect", "target": "target", "effect": "minecraft:poison", "amplifier": 0, "duration": 40},
    {"type": "set_fire", "target": "target", "seconds": 2}
  ]
}
```

## 例: ダメージを受けると25%で回復効果

```json
{
  "id": "my_pack:second_wind",
  "name": "起死回生",
  "description": "ダメージを受けると、時々再生する。",
  "rarity": "RARE",
  "obtainable": true,
  "toggleable": true,
  "triggers": [
    {
      "event": "on_hurt",
      "chance": 0.25,
      "cooldown": 100,
      "actions": [
        {"type": "effect", "effect": "minecraft:regeneration", "amplifier": 0, "duration": 80}
      ]
    }
  ]
}
```

外部能力パックはサーバー側で読み込んでください。マルチプレイではサーバーを再起動すると反映されます。能力は所持中に発動し、`toggleable: true` ならGUIでONにした時だけ動きます。`toggleable: false` は所持中ずっと有効です。連鎖ダメージによる無限ループを防ぐため、アクションから発生した追加イベントは再トリガーしません。