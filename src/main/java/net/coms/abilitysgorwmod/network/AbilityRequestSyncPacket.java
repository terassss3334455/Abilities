package net.coms.abilitysgorwmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityRequestSyncPacket {

    public AbilityRequestSyncPacket() {
    }

    public static void encode(
            AbilityRequestSyncPacket packet,
            FriendlyByteBuf buffer
    ) {
        // 送信データなし
    }

    public static AbilityRequestSyncPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new AbilityRequestSyncPacket();
    }

    public static void handle(
            AbilityRequestSyncPacket packet,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                AbilityNetwork.syncPlayer(player);
            }
        });

        context.setPacketHandled(true);
    }
}