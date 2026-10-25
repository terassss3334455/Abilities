package net.coms.abilitysgorwmod.pack;

import com.google.gson.JsonObject;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Whitelisted data-driven potion effects; arbitrary Java execution is intentionally unsupported. */
public final class AbilityEffectDefinitions {
    private static final Map<String, JsonObject> DEFINITIONS = new ConcurrentHashMap<>();
    private AbilityEffectDefinitions() {}
    static void register(String id, JsonObject json) { DEFINITIONS.put(id, json.deepCopy()); }
    public static void tick(Player player, String id) {
        JsonObject def = DEFINITIONS.get(id);
        if (def == null || player.level().isClientSide) return;
        String type = def.get("type").getAsString();
        MobEffect effect = switch (type) {
            case "speed" -> MobEffects.MOVEMENT_SPEED;
            case "haste" -> MobEffects.DIG_SPEED;
            case "strength" -> MobEffects.DAMAGE_BOOST;
            case "jump_boost" -> MobEffects.JUMP;
            case "night_vision" -> MobEffects.NIGHT_VISION;
            case "fire_resistance" -> MobEffects.FIRE_RESISTANCE;
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "slow_falling" -> MobEffects.SLOW_FALLING;
            case "regeneration" -> MobEffects.REGENERATION;
            case "resistance" -> MobEffects.DAMAGE_RESISTANCE;
            case "invisibility" -> MobEffects.INVISIBILITY;
            case "luck" -> MobEffects.LUCK;
            default -> null;
        };
        if (effect == null) return;
        int amplifier = def.has("amplifier") ? Math.max(0, Math.min(10, def.get("amplifier").getAsInt())) : 0;
        int duration = def.has("duration") ? Math.max(1, Math.min(1200, def.get("duration").getAsInt())) : 100;
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < 20)
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
    }
}
