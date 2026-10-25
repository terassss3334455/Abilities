package net.coms.abilitysgorwmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class VaultClientSync {
    private static CompoundTag data = new CompoundTag();

    private VaultClientSync() {}

    public static void apply(CompoundTag snapshot, boolean openScreen) {
        data = snapshot.copy();
        Minecraft minecraft = Minecraft.getInstance();
        if (openScreen && minecraft.player != null) {
            minecraft.setScreen(new VaultScreen());
        }
    }

    public static ItemStack getStorage(int slot) {
        return read(data.getList("storage", Tag.TAG_COMPOUND), slot);
    }

    public static ItemStack getUpgrade(int slot) {
        return read(data.getList("upgrades", Tag.TAG_COMPOUND), slot);
    }

    private static ItemStack read(ListTag list, int slot) {
        if (slot < 0 || slot >= list.size()) return ItemStack.EMPTY;
        CompoundTag entry = list.getCompound(slot);
        return entry.contains("stack", Tag.TAG_COMPOUND)
                ? ItemStack.of(entry.getCompound("stack")) : ItemStack.EMPTY;
    }

    public static int getUnlockedSlots() {
        return data.getInt("unlockedSlots");
    }

    public static boolean isAutoPickup() {
        return data.getBoolean("autoPickup");
    }
}
