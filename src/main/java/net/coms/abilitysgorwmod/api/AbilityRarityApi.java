package net.coms.abilitysgorwmod.api;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public registration and weighted-selection API for ability rarity tiers. */
public final class AbilityRarityApi {
    private AbilityRarityApi() {}

    public static AbilityDefinition.Rarity registerRarity(
            String id, Component displayName, int sortOrder, int color, double rollWeight) {
        return AbilityDefinition.Rarity.register(id, displayName, sortOrder, color, rollWeight);
    }

    public static AbilityDefinition.Rarity getRarity(String id) {
        return AbilityDefinition.Rarity.valueOf(id);
    }

    public static List<AbilityDefinition.Rarity> getRarities() {
        return List.of(AbilityDefinition.Rarity.values());
    }

    public static void setRollWeight(AbilityDefinition.Rarity rarity, double weight) {
        if (rarity == null) throw new IllegalArgumentException("rarity cannot be null");
        rarity.setRollWeight(weight);
    }

    public static List<AbilityDefinition> getAbilities(AbilityDefinition.Rarity rarity) {
        if (rarity == null) return List.of();
        return AbilityRegistry.getAll().stream()
                .filter(ability -> ability.getRarity() == rarity)
                .toList();
    }

    public static List<AbilityDefinition> getAvailableObtainableAbilities(
            Player player, AbilityDefinition.Rarity minimum, AbilityDefinition.Rarity maximum) {
        if (player == null) return List.of();
        int minOrder = minimum == null ? Integer.MIN_VALUE : minimum.ordinal();
        int maxOrder = maximum == null ? Integer.MAX_VALUE : maximum.ordinal();
        if (minOrder > maxOrder) return List.of();
        return AbilityRegistry.getAll().stream()
                .filter(AbilityDefinition::isObtainable)
                .filter(ability -> !PlayerAbilityData.hasAbility(player, ability.getId()))
                .filter(ability -> ability.getRarity().ordinal() >= minOrder
                        && ability.getRarity().ordinal() <= maxOrder)
                .toList();
    }

    /**
     * Selects a rarity tier by its configured weight, then uniformly selects one
     * available ability from that tier. Returns null when no eligible tier has weight.
     */
    public static AbilityDefinition rollRandomObtainableAbility(
            Player player, AbilityDefinition.Rarity minimum, AbilityDefinition.Rarity maximum) {
        if (player == null) return null;
        List<AbilityDefinition> available = getAvailableObtainableAbilities(player, minimum, maximum);
        if (available.isEmpty()) return null;

        Map<AbilityDefinition.Rarity, List<AbilityDefinition>> byRarity = new LinkedHashMap<>();
        for (AbilityDefinition ability : available)
            byRarity.computeIfAbsent(ability.getRarity(), ignored -> new ArrayList<>()).add(ability);

        double totalWeight = byRarity.keySet().stream()
                .mapToDouble(AbilityDefinition.Rarity::getRollWeight).sum();
        if (!(totalWeight > 0) || !Double.isFinite(totalWeight)) return null;

        double choice = player.getRandom().nextDouble() * totalWeight;
        for (Map.Entry<AbilityDefinition.Rarity, List<AbilityDefinition>> entry : byRarity.entrySet()) {
            choice -= entry.getKey().getRollWeight();
            if (choice < 0) {
                List<AbilityDefinition> tier = entry.getValue();
                return tier.get(player.getRandom().nextInt(tier.size()));
            }
        }
        List<AbilityDefinition> lastTier = new ArrayList<>(byRarity.values()).get(byRarity.size() - 1);
        return lastTier.get(player.getRandom().nextInt(lastTier.size()));
    }
}