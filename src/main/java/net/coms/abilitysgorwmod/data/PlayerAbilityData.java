package net.coms.abilitysgorwmod.data;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.api.event.AbilityApiEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public final class PlayerAbilityData {
    private static final String ROOT = "abilitysgorwmod_data";
    private static final String INITIALIZED = "initialized";
    private static final String OWNED = "owned";
    private static final String CUSTOM_DATA = "ability_custom_data";
    private static final String ID = "id";
    private static final String ACTIVE = "active";

    private PlayerAbilityData() {}

    private static CompoundTag getRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    public static boolean isInitialized(Player player) {
        return getRoot(player).getBoolean(INITIALIZED);
    }

    public static void setInitialized(Player player, boolean value) {
        getRoot(player).putBoolean(INITIALIZED, value);
    }

    private static ListTag getOwnedList(Player player) {
        CompoundTag root = getRoot(player);
        if (!root.contains(OWNED, Tag.TAG_LIST)) root.put(OWNED, new ListTag());
        return root.getList(OWNED, Tag.TAG_COMPOUND);
    }

    public static List<String> getOwnedIds(Player player) {
        List<String> result = new ArrayList<>();
        ListTag list = getOwnedList(player);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString(ID);
            if (!id.isEmpty()) result.add(id);
        }
        return result;
    }

    public static boolean hasAbility(Player player, String abilityId) {
        if (player == null || abilityId == null) return false;
        for (String id : getOwnedIds(player)) if (id.equals(abilityId)) return true;
        return false;
    }

    public static boolean addAbility(Player player, String abilityId) {
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        if (player == null || definition == null || hasAbility(player, abilityId)) return false;

        CompoundTag entry = new CompoundTag();
        entry.putString(ID, abilityId);
        entry.putBoolean(ACTIVE, false);
        getOwnedList(player).add(entry);
        if (!player.level().isClientSide)
            MinecraftForge.EVENT_BUS.post(new AbilityApiEvents.AbilityAcquiredEvent(player, definition));
        return true;
    }

    public static boolean removeAbility(Player player, String abilityId) {
        if (player == null || abilityId == null) return false;
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        if (definition == null) return false;

        ListTag list = getOwnedList(player);
        for (int i = 0; i < list.size(); i++) {
            if (!list.getCompound(i).getString(ID).equals(abilityId)) continue;
            CompoundTag previousData = getCustomData(player, abilityId);
            list.remove(i);
            if (player.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND)) {
                CompoundTag root = getRoot(player);
                if (root.contains(CUSTOM_DATA, Tag.TAG_COMPOUND)) {
                    CompoundTag allData = root.getCompound(CUSTOM_DATA);
                    allData.remove(abilityId);
                    if (allData.isEmpty()) root.remove(CUSTOM_DATA);
                }
            }
            if (!player.level().isClientSide)
                MinecraftForge.EVENT_BUS.post(new AbilityApiEvents.AbilityRemovedEvent(player, definition, previousData));
            return true;
        }
        return false;
    }

    public static boolean isActive(Player player, String abilityId) {
        if (player == null || abilityId == null) return false;
        ListTag list = getOwnedList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getString(ID).equals(abilityId)) return entry.getBoolean(ACTIVE);
        }
        return false;
    }

    public static boolean setActive(Player player, String abilityId, boolean active) {
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        if (player == null || abilityId == null || definition == null
                || !definition.isToggleable() || !hasAbility(player, abilityId)) return false;

        ListTag list = getOwnedList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString(ID).equals(abilityId)) continue;
            if (entry.getBoolean(ACTIVE) != active) {
                entry.putBoolean(ACTIVE, active);
                if (!player.level().isClientSide)
                    MinecraftForge.EVENT_BUS.post(new AbilityApiEvents.AbilityToggledEvent(player, definition, active));
            }
            return true;
        }
        return false;
    }

    /** Returns a copy of the custom NBT data for an ability owned by the player. */
    public static CompoundTag getCustomData(Player player, String abilityId) {
        if (!hasAbility(player, abilityId)) return new CompoundTag();
        CompoundTag root = getRoot(player);
        if (!root.contains(CUSTOM_DATA, Tag.TAG_COMPOUND)) return new CompoundTag();
        return root.getCompound(CUSTOM_DATA).getCompound(abilityId).copy();
    }

    /** Stores a copy of custom NBT data for an ability owned by the player. */
    public static boolean setCustomData(Player player, String abilityId, CompoundTag data) {
        if (player == null || data == null || !hasAbility(player, abilityId)) return false;
        CompoundTag root = getRoot(player);
        if (!root.contains(CUSTOM_DATA, Tag.TAG_COMPOUND)) root.put(CUSTOM_DATA, new CompoundTag());
        root.getCompound(CUSTOM_DATA).put(abilityId, data.copy());
        return true;
    }

    public static CompoundTag getSyncData(Player player) {
        return getRoot(player).copy();
    }

    public static void applySyncData(Player player, CompoundTag data) {
        player.getPersistentData().put(ROOT, data.copy());
    }

    public static void copyData(Player original, Player clone) {
        CompoundTag oldData = original.getPersistentData();
        if (oldData.contains(ROOT, Tag.TAG_COMPOUND))
            clone.getPersistentData().put(ROOT, oldData.getCompound(ROOT).copy());
    }
}