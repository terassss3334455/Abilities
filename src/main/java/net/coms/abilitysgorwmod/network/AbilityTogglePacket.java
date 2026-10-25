package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityTogglePacket {

    private final String abilityId;
    private final boolean active;

    public AbilityTogglePacket(String abilityId, boolean active) {
        this.abilityId = abilityId;
        this.active = active;
    }

    public static void encode(
            AbilityTogglePacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUtf(packet.abilityId, 128);
        buffer.writeBoolean(packet.active);
    }

    public static AbilityTogglePacket decode(FriendlyByteBuf buffer) {
        return new AbilityTogglePacket(
                buffer.readUtf(128),
                buffer.readBoolean()
        );
    }

    public static void handle(
            AbilityTogglePacket packet,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (PlayerAbilityData.setActive(
                    player,
                    packet.abilityId,
                    packet.active
            )) {
                AbilityNetwork.syncPlayer(player);
            }
        });

        context.setPacketHandled(true);
    }
}