package net.coms.abilitysgorwmod.ability;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AbilityRegistry {
    private static final Map<String, AbilityDefinition> ABILITIES = new LinkedHashMap<>();

    static {
        register("speed_boost", "高速移動", "移動速度が上昇する。");
        register("auto_smelt", "自動精錬", "拾ったアイテムの精錬工程を一段階だけ進める。");
        register("auto_cook_food", "食材変換", "拾った食材が精錬可能なら焼いた状態になる。");
        register("auto_furnace_fuel", "自動給炭", "かまどを開いた時、燃料がなければ石炭を補充する。");

        register("fall_negation", "落下否定", "落下ダメージを無効化する。");
        register("fire_immunity", "火炎支配", "炎・溶岩などの火炎ダメージを無効化する。");
        register("growth_on_plant", "豊穣の手", "植物を植えた際、25%の確率で成長させる。");
        register("free_enchant", "無償の魔術", "経験値使用時、25%の確率で経験値消費を無効化する。");
        register("poison_immunity", "毒喰らい", "毒状態を無効化する。");
        register("undead_slayer", "屍殺し", "アンデッドへのダメージが2倍になる。");
        register("freeze_immunity", "凍結拒絶", "凍結ダメージと凍結状態を防ぐ。");
        register("levitation_immunity", "浮遊拒絶", "浮遊状態を無効化する。");
        register("water_breathing", "水中呼吸", "水中で酸素が減らなくなる。");
        register("critical_strike", "会心の理", "20%の確率で与ダメージが2倍になる。");
        register("triple_luck", "幸運増幅", "精錬可能なアイテムのドロップ数を増やす。");
        register("dark_invisibility", "透過迷彩", "夜間や暗所で透明化する。");
        register("crop_growth", "収穫促進", "作物を右クリックすると25%の確率で成長する。");
        register("damage_dodge", "幸運回避", "20%の確率で受けるダメージを無効化する。");
        register("projectile_phase", "瞬間転位", "飛び道具を回避する");
        register("no_hunger", "無尽の胃袋", "空腹ゲージが減らなくなる。");
        register("night_power", "夜の覇者", "夜間に攻撃力と移動速度が上昇する。");
        register("durability_saver", "不滅の道具", "ツールの耐久値消費を25%の確率で防ぐ。");
        register("treasure_fishing", "釣運", "釣りでお宝が出やすくなる。");
        register("friendly_aura", "友愛の気配", "近くの友好モブがプレイヤーへ近づく。");
        register("piglin_peace", "ピグリンの盟約", "ピグリンから敵対されにくくなる。");
        register("explosion_immunity", "爆発否定", "爆発ダメージを無効化する。");
        register("creeper_repellent", "捕食者の威圧", "近くのクリーパーがプレイヤーを狙わなくなり消滅する。");
        register("slow_fall", "低速落下", "落下中に低速落下の効果を得る。");
        register("ender_eye_safe", "永劫の眼", "エンダーアイを投げても壊れなくなる。");
        register("water_speed", "水流加速", "水中での移動速度が上昇する。");
        register("chest_finder", "隠された座標", "近くのチェストの位置を探知する。");
        register("jump_boost", "跳躍強化", "ジャンプ力が上昇する。");
        register("knockback_immunity", "不動の理", "ノックバックしなくなる。");
        register("mining_speed", "高速採掘", "採掘速度が上昇する。");
        register("fatal_negation", "致命拒絶", "最大体力以上のダメージを与える攻撃を無効化する。");
        register("flight", "飛翔", "サバイバルでも自由に飛行できる。");

        register("auto_counterattack", "自動反撃", "20%の確率で攻撃された時に自動反撃をする能力。");
        register("double_mob_drops", "複製収穫", "モブを倒した時、25%の確率でドロップが2倍になる。");
        register("death_explosion", "最期の閃光", "死亡時に帯電クリーパー級の爆発を起こす。");
        register("dimension_shift", "境界渡り", "Hキーで現世とネザーを移動する。");
        register("random_food_effect", "食の福音", "食べ物を食べるとランダムな良い効果を得る。");
        register("grass_mineral", "草原の幸運", "草を刈ると3%の確率で鉱石類が出現する。");
        register("villager_discount", "交渉上手", "村人との取引価格が下がる。");
        register("night_vision", "暗視の瞳", "暗い場所でも周囲が見える。");
        register("entity_glow", "万象の輪郭", "近くのエンティティに発光効果を付与する。");
        register("lifesteal", "生命吸収", "攻撃を命中させると体力を2回復する。");
        register(new AbilityDefinition("charge", "突進", "視点方向へ突進して、進路上のモブに現在の攻撃力分のダメージを与える。", true, true, AbilityDefinition.Rarity.EPIC));
        register(new AbilityDefinition("resentment", "怨念", "攻撃した相手にランダムな状態異常を与える。", true, true, AbilityDefinition.Rarity.RARE));
        register("attack_speed", "連撃加速", "攻撃速度が上昇する。");
        register("phantom_repellent", "夢喰い拒絶", "近くのファントムを白い煙とともに消滅させる。");
        register("weakness_immunity", "弱体拒絶", "弱体化を無効化する。");
        register("wither_immunity", "衰弱拒絶", "衰弱を無効化する。");
        register("blindness_immunity", "盲目拒絶", "盲目を無効化する。");
        register("slowness_immunity", "鈍足拒絶", "移動速度低下を無効化する。");
        register("hunger_immunity", "飢餓拒絶", "空腹効果を無効化する。");
        register("mining_fatigue_immunity", "採掘疲労拒絶", "採掘速度低下を無効化する。");
        register("nausea_immunity", "混乱拒絶", "吐き気を無効化する。");
        register("darkness_immunity", "暗闇拒絶", "暗闇を無効化する。");
        register("unluck_immunity", "不運拒絶", "不運を無効化する。");

        register("vault_inventory", "無限収納",
                "Bキーで専用収納を開く。チェストで最大10個分まで拡張でき、自動回収も切り替えられる。");
        register(new AbilityDefinition("dancing", "ダンシング", "登録済みプレイヤーアニメーションを再生する。", true, true, AbilityDefinition.Rarity.EPIC));
        // External pack definitions are loaded after built-in IDs are registered.
        net.coms.abilitysgorwmod.pack.AbilityPackLoader.loadInto(ABILITIES);
    }

    private AbilityRegistry() {}

    private static void register(String id, String name, String description) {
        register(new AbilityDefinition(
                id,
                name,
                description,
                true,
                true,
                defaultRarity(id)
        ));
    }

    private static AbilityDefinition.Rarity defaultRarity(String id) {
        return switch (id) {
            case "fatal_negation", "flight", "dimension_shift",
                 "vault_inventory", "death_explosion" ->
                    AbilityDefinition.Rarity.LEGENDARY;

            case "projectile_phase", "triple_luck", "lifesteal",
                 "attack_speed", "auto_counterattack",
                 "double_mob_drops", "villager_discount",
                 "creeper_repellent", "phantom_repellent",
                 "night_power" ->
                    AbilityDefinition.Rarity.EPIC;

            case "speed_boost", "auto_smelt", "auto_cook_food",
                 "free_enchant", "critical_strike",
                 "damage_dodge", "mining_speed", "jump_boost",
                 "water_speed", "growth_on_plant",
                 "crop_growth", "treasure_fishing",
                 "chest_finder", "night_vision",
                 "entity_glow", "grass_mineral" ->
                    AbilityDefinition.Rarity.RARE;

            default -> AbilityDefinition.Rarity.COMMON;
        };
    }

    public static synchronized void register(AbilityDefinition ability) {
        if (ABILITIES.containsKey(ability.getId())) {
            throw new IllegalArgumentException(
                    "Duplicate ability ID: " + ability.getId()
            );
        }
        ABILITIES.put(ability.getId(), ability);
    }

    public static synchronized AbilityDefinition get(String id) {
        return ABILITIES.get(id);
    }

    public static synchronized Collection<AbilityDefinition> getAll() {
        return List.copyOf(ABILITIES.values());
    }

    public static synchronized List<AbilityDefinition> getObtainableAbilities() {
        List<AbilityDefinition> result = new ArrayList<>();

        for (AbilityDefinition ability : ABILITIES.values()) {
            if (ability.isObtainable()) {
                result.add(ability);
            }
        }

        return result;
    }

    public static synchronized List<AbilityDefinition> getUnownedObtainableAbilities(Player player) {
        return getUnownedObtainableAbilities(
                player,
                AbilityDefinition.Rarity.COMMON
        );
    }

    /**
     * 指定レアリティ以下の未所持能力を取得する
     */
    public static synchronized List<AbilityDefinition> getUnownedObtainableAbilities(
            Player player,
            AbilityDefinition.Rarity maximumRarity
    ) {
        List<AbilityDefinition> result = new ArrayList<>();

        AbilityDefinition.Rarity maximum = maximumRarity == null
                ? AbilityDefinition.Rarity.COMMON
                : maximumRarity;

        for (AbilityDefinition ability : ABILITIES.values()) {
            if (ability.isObtainable()
                    && ability.getRarity().ordinal() <= maximum.ordinal()
                    && !PlayerAbilityData.hasAbility(player, ability.getId())) {
                result.add(ability);
            }
        }

        return result;
    }
}