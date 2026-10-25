package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.ability.ChargeAbility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ChargeRequestPacket {
    public static void encode(ChargeRequestPacket packet, FriendlyByteBuf buffer) {}
    public static ChargeRequestPacket decode(FriendlyByteBuf buffer) { return new ChargeRequestPacket(); }

    public static void handle(ChargeRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ChargeAbility.activate(player);
            AbilityCooldownSyncPacket.send(player);
        });
        context.setPacketHandled(true);
    }
}