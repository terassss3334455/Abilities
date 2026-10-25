package net.coms.abilitysgorwmod.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.api.event.AbilityApiEvents;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Safe, data-driven event triggers for external ability packs. */
public final class AbilityTriggerRules {
    private static final Set<String> EVENTS = Set.of(
            "on_hurt", "on_attack", "on_kill", "on_tick", "on_jump",
            "on_item_use", "on_pickup", "on_block_break"
    );
    private static final Set<String> ACTIONS = Set.of(
            "effect", "remove_effect", "set_fire", "heal", "damage",
            "cancel_damage", "give_item", "feed"
    );
    private static final Set<String> TARGETS = Set.of("self", "attacker", "target");
    private static final Map<String, List<JsonObject>> RULES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> false);

    private AbilityTriggerRules() {}

    public record Context(Entity target, Entity attacker, LivingHurtEvent hurtEvent, ItemStack item) {
        public Context {
            item = item == null ? ItemStack.EMPTY : item;
        }
    }

    public static void validate(JsonArray triggers) {
        if (triggers == null) return;
        if (triggers.size() > 16) throw new IllegalArgumentException("triggers は最大16個です");
        for (JsonElement element : triggers) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("trigger はオブジェクトで指定してください");
            JsonObject trigger = element.getAsJsonObject();
            onlyKeys(trigger, Set.of("event", "chance", "cooldown", "item", "damage_type", "min_damage", "max_damage",
                    "target_entity", "health_below", "health_above", "actions"), "trigger");
            String event = requiredString(trigger, "event");
            if (!EVENTS.contains(event)) throw new IllegalArgumentException("未対応の trigger.event: " + event);
            if (trigger.has("chance")) requireNumberRange(trigger, "chance", 0, 1);
            if (trigger.has("cooldown")) requireIntegerRange(trigger, "cooldown", 0, 72000);
            if (trigger.has("item")) {
                if (!event.equals("on_item_use") && !event.equals("on_pickup"))
                    throw new IllegalArgumentException("trigger.item は on_item_use / on_pickup でのみ使えます");
                requireResourceLocation(trigger, "item");
            }
            if (trigger.has("damage_type") || trigger.has("min_damage") || trigger.has("max_damage")) {
                if (!event.equals("on_hurt") && !event.equals("on_attack"))
                    throw new IllegalArgumentException("damage_type / min_damage / max_damage は on_hurt / on_attack でのみ使えます");
                if (trigger.has("damage_type")) requireResourceLocation(trigger, "damage_type");
                if (trigger.has("min_damage")) requireNumberRange(trigger, "min_damage", 0, 1000);
                if (trigger.has("max_damage")) requireNumberRange(trigger, "max_damage", 0, 1000);
                if (trigger.has("min_damage") && trigger.has("max_damage")
                        && trigger.get("min_damage").getAsDouble() > trigger.get("max_damage").getAsDouble())
                    throw new IllegalArgumentException("min_damage は max_damage 以下にしてください");
            }
            if (trigger.has("target_entity")) {
                if (!event.equals("on_attack") && !event.equals("on_kill"))
                    throw new IllegalArgumentException("target_entity は on_attack / on_kill でのみ使えます");
                requireResourceLocation(trigger, "target_entity");
            }
            if (trigger.has("health_below")) requireNumberRange(trigger, "health_below", 0, 2048);
            if (trigger.has("health_above")) requireNumberRange(trigger, "health_above", 0, 2048);
            if (trigger.has("health_below") && trigger.has("health_above")
                    && trigger.get("health_above").getAsDouble() >= trigger.get("health_below").getAsDouble())
                throw new IllegalArgumentException("health_above は health_below より小さくしてください");
            if (!trigger.has("actions") || !trigger.get("actions").isJsonArray())
                throw new IllegalArgumentException("trigger.actions は配列で指定してください");
            JsonArray actions = trigger.getAsJsonArray("actions");
            if (actions.size() < 1 || actions.size() > 16)
                throw new IllegalArgumentException("actions は1～16個にしてください");
            for (JsonElement actionElement : actions) {
                if (!actionElement.isJsonObject()) throw new IllegalArgumentException("action はオブジェクトで指定してください");
                validateAction(actionElement.getAsJsonObject(), event);
            }
        }
    }

    private static void validateAction(JsonObject action, String event) {
        if (!action.has("type")) throw new IllegalArgumentException("action.type がありません");
        String type = requiredString(action, "type");
        if (!ACTIONS.contains(type)) throw new IllegalArgumentException("未対応の action.type: " + type);
        switch (type) {
            case "effect" -> {
                onlyKeys(action, Set.of("type", "target", "effect", "amplifier", "duration"), "effect action");
                requireTarget(action);
                requireResourceLocation(action, "effect");
                if (action.has("amplifier")) requireIntegerRange(action, "amplifier", 0, 10);
                if (action.has("duration")) requireIntegerRange(action, "duration", 1, 1200);
            }
            case "remove_effect" -> {
                onlyKeys(action, Set.of("type", "target", "effect"), "remove_effect action");
                requireTarget(action);
                requireResourceLocation(action, "effect");
            }
            case "set_fire" -> {
                onlyKeys(action, Set.of("type", "target", "seconds"), "set_fire action");
                requireTarget(action);
                if (action.has("seconds")) requireIntegerRange(action, "seconds", 1, 300);
            }
            case "heal", "damage" -> {
                onlyKeys(action, Set.of("type", "target", "amount"), type + " action");
                requireTarget(action);
                requireNumberRange(action, "amount", 0.01, 100);
            }
            case "cancel_damage" -> {
                onlyKeys(action, Set.of("type"), "cancel_damage action");
                if (!event.equals("on_hurt") && !event.equals("on_attack"))
                    throw new IllegalArgumentException("cancel_damage は on_hurt / on_attack でのみ使えます");
            }
            case "give_item" -> {
                onlyKeys(action, Set.of("type", "item", "count"), "give_item action");
                requireResourceLocation(action, "item");
                if (action.has("count")) requireIntegerRange(action, "count", 1, 64);
            }
            case "feed" -> {
                onlyKeys(action, Set.of("type", "food", "saturation"), "feed action");
                requireIntegerRange(action, "food", 0, 20);
                if (action.has("saturation")) requireNumberRange(action, "saturation", 0, 20);
            }
            default -> throw new IllegalArgumentException("未対応の action.type: " + type);
        }
    }

    public static void register(String abilityId, JsonArray triggers) {
        if (triggers == null || triggers.isEmpty()) {
            RULES.remove(abilityId);
            return;
        }
        List<JsonObject> copy = new ArrayList<>();
        for (JsonElement trigger : triggers) copy.add(trigger.getAsJsonObject().deepCopy());
        RULES.put(abilityId, List.copyOf(copy));
    }

    public static boolean isEnabled(Player owner, String abilityId) {
        if (owner == null || !PlayerAbilityData.hasAbility(owner, abilityId)) return false;
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        return definition != null && (!definition.isToggleable() || PlayerAbilityData.isActive(owner, abilityId));
    }

    public record CooldownState(String abilityId, int remainingTicks, int totalTicks) {}

    public static List<CooldownState> getActiveCooldowns(Player owner) {
        if (owner == null || owner.level().isClientSide) return List.of();
        long now = owner.level().getGameTime();
        Map<String, CooldownState> active = new LinkedHashMap<>();
        for (String abilityId : PlayerAbilityData.getOwnedIds(owner)) {
            if (!isEnabled(owner, abilityId)) continue;
            List<JsonObject> triggers = RULES.get(abilityId);
            if (triggers == null) continue;
            for (int index = 0; index < triggers.size(); index++) {
                JsonObject trigger = triggers.get(index);
                int cooldown = trigger.has("cooldown") ? trigger.get("cooldown").getAsInt()
                        : ("on_tick".equals(trigger.get("event").getAsString()) ? 20 : 0);
                if (cooldown <= 0) continue;
                long remainingLong = owner.getPersistentData().getLong(cooldownKey(abilityId, index)) - now;
                if (remainingLong <= 0) continue;
                int remaining = (int) Math.min(Integer.MAX_VALUE, remainingLong);
                CooldownState previous = active.get(abilityId);
                active.put(abilityId, new CooldownState(abilityId,
                        Math.max(remaining, previous == null ? 0 : previous.remainingTicks()),
                        Math.max(cooldown, previous == null ? 0 : previous.totalTicks())));
            }
        }
        return List.copyOf(active.values());
    }
    public static void fire(Player owner, String event, Context context) {
        if (owner == null || owner.level().isClientSide || EXECUTING.get()) return;
        List<String> ownedIds = PlayerAbilityData.getOwnedIds(owner);
        if (ownedIds.isEmpty()) return;
        EXECUTING.set(true);
        try {
            for (String abilityId : ownedIds) {
                if (!isEnabled(owner, abilityId)) continue;
                List<JsonObject> triggers = RULES.get(abilityId);
                if (triggers == null) continue;
                for (int index = 0; index < triggers.size(); index++) {
                    JsonObject trigger = triggers.get(index);
                    if (!event.equals(trigger.get("event").getAsString()) || !matchesItem(trigger, context)
                            || !matchesDamage(trigger, context) || !matchesTargetType(trigger, context)
                            || !matchesHealth(owner, trigger)) continue;
                    double chance = trigger.has("chance") ? trigger.get("chance").getAsDouble() : 1.0;
                    if (chance < 1.0 && owner.getRandom().nextDouble() >= chance) continue;
                    int cooldown = trigger.has("cooldown") ? trigger.get("cooldown").getAsInt() : (event.equals("on_tick") ? 20 : 0);
                    String cooldownKey = cooldownKey(abilityId, index);
                    long now = owner.level().getGameTime();
                    if (cooldown > 0 && owner.getPersistentData().getLong(cooldownKey) > now) continue;
                    AbilityDefinition definition = AbilityRegistry.get(abilityId);
                    if (definition == null) continue;
                    AbilityApiEvents.AbilityTriggeredEvent triggered =
                            new AbilityApiEvents.AbilityTriggeredEvent(owner, definition, event, context);
                    if (MinecraftForge.EVENT_BUS.post(triggered)) continue;
                    if (cooldown > 0) owner.getPersistentData().putLong(cooldownKey, now + cooldown);
                    for (JsonElement action : trigger.getAsJsonArray("actions")) execute(owner, action.getAsJsonObject(), context);
                }
            }
        } finally {
            EXECUTING.set(false);
        }
    }

    private static boolean matchesItem(JsonObject trigger, Context context) {
        if (!trigger.has("item")) return true;
        if (context == null || context.item().isEmpty()) return false;
        ResourceLocation actual = ForgeRegistries.ITEMS.getKey(context.item().getItem());
        ResourceLocation expected = ResourceLocation.tryParse(trigger.get("item").getAsString());
        return actual != null && actual.equals(expected);
    }

    private static boolean matchesDamage(JsonObject trigger, Context context) {
        if (!trigger.has("damage_type") && !trigger.has("min_damage") && !trigger.has("max_damage")) return true;
        if (context == null || context.hurtEvent() == null) return false;
        if (trigger.has("damage_type")) {
            ResourceLocation actual = context.hurtEvent().getSource().typeHolder().unwrapKey().map(key -> key.location()).orElse(null);
            ResourceLocation expected = ResourceLocation.tryParse(trigger.get("damage_type").getAsString());
            if (actual == null || !actual.equals(expected)) return false;
        }
        float amount = context.hurtEvent().getAmount();
        if (trigger.has("min_damage") && amount < trigger.get("min_damage").getAsFloat()) return false;
        if (trigger.has("max_damage") && amount > trigger.get("max_damage").getAsFloat()) return false;
        return true;
    }
    private static boolean matchesTargetType(JsonObject trigger, Context context) {
        if (!trigger.has("target_entity")) return true;
        if (context == null || context.target() == null) return false;
        ResourceLocation actual = ForgeRegistries.ENTITY_TYPES.getKey(context.target().getType());
        ResourceLocation expected = ResourceLocation.tryParse(trigger.get("target_entity").getAsString());
        return actual != null && actual.equals(expected);
    }

    private static boolean matchesHealth(Player owner, JsonObject trigger) {
        float health = owner.getHealth();
        if (trigger.has("health_below") && health >= trigger.get("health_below").getAsFloat()) return false;
        return !trigger.has("health_above") || health <= trigger.get("health_above").getAsFloat();
    }

    private static void execute(Player owner, JsonObject action, Context context) {
        String type = action.get("type").getAsString();
        switch (type) {
            case "effect" -> {
                LivingEntity target = livingTarget(owner, action, context);
                ResourceLocation id = ResourceLocation.tryParse(action.get("effect").getAsString());
                MobEffect effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
                if (target != null && effect != null) {
                    int duration = action.has("duration") ? action.get("duration").getAsInt() : 100;
                    int amplifier = action.has("amplifier") ? action.get("amplifier").getAsInt() : 0;
                    target.addEffect(new MobEffectInstance(effect, duration, amplifier));
                }
            }
            case "remove_effect" -> {
                LivingEntity target = livingTarget(owner, action, context);
                ResourceLocation id = ResourceLocation.tryParse(action.get("effect").getAsString());
                MobEffect effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
                if (target != null && effect != null) target.removeEffect(effect);
            }
            case "set_fire" -> {
                LivingEntity target = livingTarget(owner, action, context);
                if (target != null) target.setSecondsOnFire(action.has("seconds") ? action.get("seconds").getAsInt() : 5);
            }
            case "heal" -> {
                LivingEntity target = livingTarget(owner, action, context);
                if (target != null) target.heal(action.get("amount").getAsFloat());
            }
            case "damage" -> {
                LivingEntity target = livingTarget(owner, action, context);
                if (target != null) target.hurt(owner.damageSources().magic(), action.get("amount").getAsFloat());
            }
            case "cancel_damage" -> {
                if (context != null && context.hurtEvent() != null) context.hurtEvent().setCanceled(true);
            }
            case "give_item" -> {
                ResourceLocation id = ResourceLocation.tryParse(action.get("item").getAsString());
                Item item = id == null ? Items.AIR : ForgeRegistries.ITEMS.getValue(id);
                if (item != null && item != Items.AIR) {
                    int count = action.has("count") ? action.get("count").getAsInt() : 1;
                    ItemStack stack = new ItemStack(item, count);
                    if (!owner.getInventory().add(stack)) owner.drop(stack, false);
                }
            }
            case "feed" -> {
                int food = action.get("food").getAsInt();
                float saturation = action.has("saturation") ? action.get("saturation").getAsFloat() : 0;
                int foodLevel = Math.min(20, owner.getFoodData().getFoodLevel() + food);
                owner.getFoodData().setFoodLevel(foodLevel);
                owner.getFoodData().setSaturation(Math.min(foodLevel, owner.getFoodData().getSaturationLevel() + saturation));
            }
            default -> { }
        }
    }

    private static LivingEntity livingTarget(Player owner, JsonObject action, Context context) {
        String target = action.has("target") ? action.get("target").getAsString() : "self";
        Entity entity = switch (target) {
            case "attacker" -> context == null ? null : context.attacker();
            case "target" -> context == null ? null : context.target();
            default -> owner;
        };
        return entity instanceof LivingEntity living ? living : null;
    }

    private static String cooldownKey(String abilityId, int index) {
        String token = UUID.nameUUIDFromBytes((abilityId + "#" + index).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return "abilitysgorwmod_trigger_" + token;
    }

    private static void requireTarget(JsonObject action) {
        String target = action.has("target") ? requiredString(action, "target") : "self";
        if (!TARGETS.contains(target)) throw new IllegalArgumentException("target は self / attacker / target のいずれかです");
    }

    private static void requireResourceLocation(JsonObject object, String key) {
        String value = requiredString(object, key);
        if (ResourceLocation.tryParse(value) == null) throw new IllegalArgumentException(key + " は namespace:path 形式にしてください");
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()
                || object.get(key).getAsString().isBlank()) throw new IllegalArgumentException("必須文字列フィールドがありません: " + key);
        return object.get(key).getAsString().trim();
    }

    private static void requireIntegerRange(JsonObject object, String key, int min, int max) {
        JsonPrimitive value = numeric(object, key);
        double number = value.getAsDouble();
        if (number != Math.rint(number) || number < min || number > max)
            throw new IllegalArgumentException(key + " は " + min + "～" + max + " の整数にしてください");
    }

    private static void requireNumberRange(JsonObject object, String key, double min, double max) {
        double number = numeric(object, key).getAsDouble();
        if (!Double.isFinite(number) || number < min || number > max)
            throw new IllegalArgumentException(key + " は " + min + "～" + max + " にしてください");
    }

    private static JsonPrimitive numeric(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber())
            throw new IllegalArgumentException(key + " は数値で指定してください");
        return object.getAsJsonPrimitive(key);
    }

    private static void onlyKeys(JsonObject object, Set<String> allowed, String where) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException(where + " に未対応の項目があります: " + key);
        }
    }
}