package net.coms.abilitysgorwmod.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Base probabilities for chance-based abilities, used by the effects screen and API integrations. */
public final class AbilityChanceCatalog {
    private static final Map<String, Float> BASE_CHANCES = new LinkedHashMap<>();

    static {
        register("durability_saver", 0.25F);
        register("auto_counterattack", 0.20F);
        register("damage_dodge", 0.20F);
        register("critical_strike", 0.20F);
        register("crop_growth", 0.25F);
        register("growth_on_plant", 0.25F);
        register("grass_mineral", 0.03F);
        register("double_mob_drops", 0.25F);
        register("free_enchant", 0.25F);
    }

    private AbilityChanceCatalog() {}

    /** Registers the base probability shown for an ability. Call on both client and server. */
    public static synchronized void register(String abilityId, float baseChance) {
        if (abilityId == null || !abilityId.matches("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?")) {
            throw new IllegalArgumentException("Ability chance ID must use lowercase ID format");
        }
        if (!Float.isFinite(baseChance) || baseChance < 0.0F || baseChance > 1.0F) {
            throw new IllegalArgumentException("Base chance must be between 0 and 1");
        }
        Float existing = BASE_CHANCES.get(abilityId);
        if (existing != null && Float.compare(existing, baseChance) != 0) {
            throw new IllegalArgumentException("Conflicting base chance registered for " + abilityId);
        }
        BASE_CHANCES.putIfAbsent(abilityId, baseChance);
    }

    public static synchronized float getBaseChance(String abilityId) {
        Float value = BASE_CHANCES.get(abilityId);
        if (value == null) throw new IllegalArgumentException("No base chance registered for " + abilityId);
        return value;
    }

    public static synchronized Map<String, Float> getAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(BASE_CHANCES));
    }
}