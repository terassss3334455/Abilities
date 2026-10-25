package net.coms.abilitysgorwmod.api;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.api.event.AbilityApiEvents;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Public Java API for registering and using abilities supplied by other mods. */
public final class AbilityApi {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private AbilityApi() {}

    /** Register during your mod's common initialization on both client and server. */
    public static void registerAbility(AbilityDefinition ability) {
        if (ability == null || ability.getId() == null || !RESOURCE_ID.matcher(ability.getId()).matches())
            throw new IllegalArgumentException("Ability ID must use namespace:path format");
        if (ability.getName() == null || ability.getDescription() == null)
            throw new IllegalArgumentException("Ability name and description cannot be null");
        AbilityRegistry.register(ability);
    }

    /** Registers a base chance so the inventory effects screen can report the adjusted value. */
    public static void registerAbilityChance(String abilityId, float baseChance) {
        AbilityChanceCatalog.register(abilityId, baseChance);
    }

    /** Returns an immutable snapshot of chance values registered for the effects screen. */
    public static Map<String, Float> getRegisteredAbilityChances() {
        return AbilityChanceCatalog.getAll();
    }
    public static AbilityDefinition getAbility(String abilityId) {
        return AbilityRegistry.get(abilityId);
    }

    /**
     * Returns the effective probability for an ability, allowing other mods to adjust it through
     * {@link AbilityApiEvents.AbilityChanceEvent}. Probabilities are clamped to the inclusive 0..1 range.
     */
    public static float getAbilityChance(Player player, String abilityId, float baseChance) {
        float safeBase = clampChance(baseChance);
        if (player == null || abilityId == null || player.level().isClientSide) return safeBase;
        AbilityApiEvents.AbilityChanceEvent event =
                new AbilityApiEvents.AbilityChanceEvent(player, abilityId, safeBase);
        MinecraftForge.EVENT_BUS.post(event);
        return clampChance(event.getChance());
    }

    private static float clampChance(float chance) {
        if (Float.isNaN(chance)) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, chance));
    }
    public static List<AbilityDefinition> getAbilities() {
        return List.copyOf(AbilityRegistry.getAll());
    }

    public static List<String> getOwnedAbilityIds(Player player) {
        return player == null ? List.of() : PlayerAbilityData.getOwnedIds(player);
    }

    public static boolean hasAbility(Player player, String abilityId) {
        return player != null && PlayerAbilityData.hasAbility(player, abilityId);
    }

    public static boolean isAbilityEnabled(Player player, String abilityId) {
        if (player == null || !PlayerAbilityData.hasAbility(player, abilityId)) return false;
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        return definition != null && (!definition.isToggleable()
                || PlayerAbilityData.isActive(player, abilityId));
    }

    /** Server-side grant. Successful changes are synced to the owning client. */
    public static boolean grantAbility(Player player, String abilityId) {
        if (!isServerPlayer(player) || !PlayerAbilityData.addAbility(player, abilityId)) return false;
        sync(player);
        return true;
    }

    /** Server-side removal. Also clears this ability's custom growth data. */
    public static boolean removeAbility(Player player, String abilityId) {
        if (!isServerPlayer(player) || !PlayerAbilityData.removeAbility(player, abilityId)) return false;
        sync(player);
        return true;
    }

    /** Enables or disables a toggleable ability owned by the player. */
    public static boolean setAbilityEnabled(Player player, String abilityId, boolean enabled) {
        if (!isServerPlayer(player) || !PlayerAbilityData.setActive(player, abilityId, enabled)) return false;
        sync(player);
        return true;
    }

    /** Returns a copy of data stored for this owned ability. */
    public static CompoundTag getAbilityData(Player player, String abilityId) {
        return PlayerAbilityData.getCustomData(player, abilityId);
    }

    /** Stores a copy of data for this owned ability and syncs it to the client. */
    public static boolean setAbilityData(Player player, String abilityId, CompoundTag data) {
        if (!isServerPlayer(player) || data == null
                || !PlayerAbilityData.setCustomData(player, abilityId, data)) return false;
        sync(player);
        return true;
    }

    /**
     * Posts a cancellable activation event for an owned, enabled ability.
     * This does not toggle the ability; listeners implement the activated behavior.
     */
    public static boolean activateAbility(Player player, String abilityId, String source) {
        if (!isServerPlayer(player) || !isAbilityEnabled(player, abilityId)) return false;
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        return !MinecraftForge.EVENT_BUS.post(
                new AbilityApiEvents.AbilityActivationEvent(player, definition, source));
    }

    private static boolean isServerPlayer(Player player) {
        return player != null && !player.level().isClientSide;
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) AbilityNetwork.syncPlayer(serverPlayer);
    }
}