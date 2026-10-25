package net.coms.abilitysgorwmod.data;

import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Player-persistent storage for the vault ability. */
public final class VaultInventoryData {
    public static final int BASE_SLOTS = 27;
    public static final int MAX_UPGRADES = 10;
    public static final int MAX_SLOTS = BASE_SLOTS * (MAX_UPGRADES + 1);
    private static final String ROOT = "abilitysgorwmod_vault";
    private static final String STORAGE = "storage";
    private static final String UPGRADES = "upgrades";
    private static final String AUTO_PICKUP = "autoPickup";

    private VaultInventoryData() {}

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag root = new CompoundTag();
            root.put(STORAGE, new ListTag());
            root.put(UPGRADES, new ListTag());
            root.putBoolean(AUTO_PICKUP, false);
            persistent.put(ROOT, root);
        }
        CompoundTag root = persistent.getCompound(ROOT);
        ensureList(root, STORAGE, MAX_SLOTS);
        ensureList(root, UPGRADES, MAX_UPGRADES);
        return root;
    }

    private static void ensureList(CompoundTag root, String key, int size) {
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        while (list.size() < size) list.add(new CompoundTag());
        while (list.size() > size) list.remove(list.size() - 1);
        root.put(key, list);
    }

    private static ItemStack readStack(ListTag list, int index) {
        if (index < 0 || index >= list.size()) return ItemStack.EMPTY;
        CompoundTag entry = list.getCompound(index);
        if (!entry.contains("stack", Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.of(entry.getCompound("stack"));
    }

    private static void writeStack(ListTag list, int index, ItemStack stack) {
        if (index < 0 || index >= list.size()) return;
        CompoundTag entry = new CompoundTag();
        if (!stack.isEmpty()) entry.put("stack", stack.save(new CompoundTag()));
        list.set(index, entry);
    }

    public static int getUpgradeCount(Player player) {
        ListTag list = root(player).getList(UPGRADES, Tag.TAG_COMPOUND);
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = readStack(list, i);
            if (stack.is(Items.CHEST)) count++;
        }
        return count;
    }

    public static int getUnlockedSlots(Player player) {
        return BASE_SLOTS * (1 + getUpgradeCount(player));
    }

    public static ItemStack getStorageItem(Player player, int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) return ItemStack.EMPTY;
        return readStack(root(player).getList(STORAGE, Tag.TAG_COMPOUND), slot);
    }

    public static void setStorageItem(Player player, int slot, ItemStack stack) {
        if (slot < 0 || slot >= getUnlockedSlots(player)) return;
        writeStack(root(player).getList(STORAGE, Tag.TAG_COMPOUND), slot, stack.copy());
    }

    public static ItemStack getUpgradeItem(Player player, int slot) {
        if (slot < 0 || slot >= MAX_UPGRADES) return ItemStack.EMPTY;
        return readStack(root(player).getList(UPGRADES, Tag.TAG_COMPOUND), slot);
    }

    /** Upgrade slots are one-way: a chest can be inserted, never removed. */
    public static boolean insertUpgrade(Player player, int slot, ItemStack cursor) {
        if (slot < 0 || slot >= MAX_UPGRADES || cursor.isEmpty() || !cursor.is(Items.CHEST)
                || !getUpgradeItem(player, slot).isEmpty()) return false;
        ItemStack one = cursor.copyWithCount(1);
        writeStack(root(player).getList(UPGRADES, Tag.TAG_COMPOUND), slot, one);
        cursor.shrink(1);
        return true;
    }

    public static boolean isAutoPickup(Player player) {
        return root(player).getBoolean(AUTO_PICKUP);
    }

    public static void setAutoPickup(Player player, boolean enabled) {
        root(player).putBoolean(AUTO_PICKUP, enabled);
    }

    public static void toggleAutoPickup(Player player) {
        setAutoPickup(player, !isAutoPickup(player));
    }

    /** Inserts as much as possible and returns a stack containing any remainder. */
    public static ItemStack insertStack(Player player, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        ItemStack remainder = input.copy();
        int capacity = getUnlockedSlots(player);

        for (int i = 0; i < capacity && !remainder.isEmpty(); i++) {
            ItemStack existing = getStorageItem(player, i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remainder)) continue;
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, remainder.getCount());
            existing.grow(moved);
            remainder.shrink(moved);
            setStorageItem(player, i, existing);
        }

        for (int i = 0; i < capacity && !remainder.isEmpty(); i++) {
            if (!getStorageItem(player, i).isEmpty()) continue;
            int moved = Math.min(remainder.getMaxStackSize(), remainder.getCount());
            setStorageItem(player, i, remainder.copyWithCount(moved));
            remainder.shrink(moved);
        }
        return remainder;
    }

    public static CompoundTag getSyncData(Player player) {
        CompoundTag snapshot = new CompoundTag();
        CompoundTag root = root(player);
        snapshot.put(STORAGE, root.getList(STORAGE, Tag.TAG_COMPOUND).copy());
        snapshot.put(UPGRADES, root.getList(UPGRADES, Tag.TAG_COMPOUND).copy());
        snapshot.putBoolean(AUTO_PICKUP, root.getBoolean(AUTO_PICKUP));
        snapshot.putInt("unlockedSlots", getUnlockedSlots(player));
        return snapshot;
    }

    public static void copyData(Player original, Player clone) {
        CompoundTag old = original.getPersistentData();
        if (old.contains(ROOT, Tag.TAG_COMPOUND)) {
            clone.getPersistentData().put(ROOT, old.getCompound(ROOT).copy());
        }
    }

    public static void syncIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityNetwork.syncVault(serverPlayer, false);
        }
    }
}
