package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.data.VaultInventoryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** type: 0 vault click, 1 player click, 2 upgrade slot, 3 auto-pickup toggle,
 * 4 bulk vault-to-player, 5 bulk player-to-vault, 6 single vault-to-player,
 * 7 single player-to-vault. */
public final class VaultActionPacket {
    private final int type;
    private final int slot;
    private final int mouseButton;
    private final ItemStack itemKey;

    public VaultActionPacket(int type, int slot, int mouseButton) {
        this(type, slot, mouseButton, ItemStack.EMPTY);
    }

    public VaultActionPacket(int type, int slot, int mouseButton, ItemStack itemKey) {
        this.type = type;
        this.slot = slot;
        this.mouseButton = mouseButton;
        this.itemKey = itemKey == null ? ItemStack.EMPTY : itemKey.copyWithCount(1);
    }

    public static void encode(VaultActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.type);
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.mouseButton);
        buffer.writeItem(packet.itemKey);
    }

    public static VaultActionPacket decode(FriendlyByteBuf buffer) {
        return new VaultActionPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readItem());
    }

    public static void handle(VaultActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !PlayerAbilityData.isActive(player, "vault_inventory")) return;
            ItemStack cursor = player.containerMenu.getCarried().copy();

            if (packet.type == 3) {
                VaultInventoryData.toggleAutoPickup(player);
            } else if (packet.type == 4) {
                moveAllMatchingVaultToPlayer(player, packet.slot, packet.itemKey);
            } else if (packet.type == 5) {
                moveAllMatchingPlayerToVault(player, packet.slot, packet.itemKey);
            } else if (packet.type == 6) {
                moveSingleVaultStackToPlayer(player, packet.slot);
            } else if (packet.type == 7) {
                moveSinglePlayerStackToVault(player, packet.slot);
            } else if (packet.type == 2) {
                if (packet.mouseButton == 0 && VaultInventoryData.insertUpgrade(player, packet.slot, cursor)) {
                    player.containerMenu.setCarried(cursor);
                }
            } else if (packet.type == 0 && packet.slot >= 0 && packet.slot < VaultInventoryData.getUnlockedSlots(player)) {
                ItemStack target = VaultInventoryData.getStorageItem(player, packet.slot);
                ItemStack[] changed = click(target, cursor, packet.mouseButton);
                VaultInventoryData.setStorageItem(player, packet.slot, changed[0]);
                player.containerMenu.setCarried(changed[1]);
            } else if (packet.type == 1 && packet.slot >= 0 && packet.slot < player.getInventory().items.size()) {
                ItemStack target = player.getInventory().getItem(packet.slot).copy();
                ItemStack[] changed = click(target, cursor, packet.mouseButton);
                player.getInventory().setItem(packet.slot, changed[0]);
                player.containerMenu.setCarried(changed[1]);
            }

            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            AbilityNetwork.syncVault(player, false);
        });
        context.setPacketHandled(true);
    }

    private static void moveSingleVaultStackToPlayer(ServerPlayer player, int sourceSlot) {
        if (sourceSlot < 0 || sourceSlot >= VaultInventoryData.getUnlockedSlots(player)) return;
        ItemStack source = VaultInventoryData.getStorageItem(player, sourceSlot);
        if (source.isEmpty()) return;
        ItemStack remainder = source.copy();
        player.getInventory().add(remainder);
        VaultInventoryData.setStorageItem(player, sourceSlot, remainder);
    }

    private static void moveSinglePlayerStackToVault(ServerPlayer player, int sourceSlot) {
        if (sourceSlot < 0 || sourceSlot >= player.getInventory().items.size()) return;
        ItemStack source = player.getInventory().getItem(sourceSlot);
        if (source.isEmpty()) return;
        ItemStack remainder = VaultInventoryData.insertStack(player, source.copy());
        player.getInventory().setItem(sourceSlot, remainder);
    }

    private static void moveAllMatchingVaultToPlayer(ServerPlayer player, int sourceSlot, ItemStack requestedKey) {
        if (sourceSlot < 0 || sourceSlot >= VaultInventoryData.getUnlockedSlots(player)) return;
        ItemStack source = VaultInventoryData.getStorageItem(player, sourceSlot);
        ItemStack key = requestedKey.isEmpty() ? source.copyWithCount(1) : requestedKey.copyWithCount(1);
        if (key.isEmpty()) return;
        for (int i = 0; i < VaultInventoryData.getUnlockedSlots(player); i++) {
            ItemStack candidate = VaultInventoryData.getStorageItem(player, i);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, key)) continue;
            ItemStack remainder = candidate.copy();
            player.getInventory().add(remainder);
            VaultInventoryData.setStorageItem(player, i, remainder);
        }
    }

    private static void moveAllMatchingPlayerToVault(ServerPlayer player, int sourceSlot, ItemStack requestedKey) {
        if (sourceSlot < 0 || sourceSlot >= player.getInventory().items.size()) return;
        ItemStack source = player.getInventory().getItem(sourceSlot);
        ItemStack key = requestedKey.isEmpty() ? source.copyWithCount(1) : requestedKey.copyWithCount(1);
        if (key.isEmpty()) return;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, key)) continue;
            ItemStack remainder = VaultInventoryData.insertStack(player, candidate.copy());
            player.getInventory().setItem(i, remainder);
        }
    }

    /** Returns {new slot stack, new cursor stack}; button 0=left, 1=right. */
    private static ItemStack[] click(ItemStack targetInput, ItemStack cursorInput, int button) {
        ItemStack target = targetInput.copy();
        ItemStack cursor = cursorInput.copy();
        if (button != 0 && button != 1) return new ItemStack[]{target, cursor};

        if (cursor.isEmpty()) {
            if (target.isEmpty()) return new ItemStack[]{target, cursor};
            if (button == 0) {
                cursor = target;
                target = ItemStack.EMPTY;
            } else {
                int take = (target.getCount() + 1) / 2;
                cursor = target.copyWithCount(take);
                target.shrink(take);
            }
        } else if (target.isEmpty()) {
            int amount = button == 0 ? Math.min(cursor.getCount(), cursor.getMaxStackSize()) : 1;
            target = cursor.copyWithCount(amount);
            cursor.shrink(amount);
        } else if (ItemStack.isSameItemSameTags(target, cursor)
                && target.getCount() < target.getMaxStackSize()) {
            int amount = button == 0
                    ? Math.min(cursor.getCount(), target.getMaxStackSize() - target.getCount())
                    : 1;
            amount = Math.min(amount, target.getMaxStackSize() - target.getCount());
            target.grow(amount);
            cursor.shrink(amount);
        } else if (button == 0) {
            ItemStack temp = target;
            target = cursor;
            cursor = temp;
        }
        return new ItemStack[]{target, cursor};
    }
}
