package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.ability.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenVaultPacket {
    public static void encode(OpenVaultPacket packet, FriendlyByteBuf buffer) {}
    public static OpenVaultPacket decode(FriendlyByteBuf buffer) { return new OpenVaultPacket(); }

    public static void handle(OpenVaultPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && PlayerAbilityData.isActive(player, "vault_inventory")) {
                AbilityNetwork.syncVault(player, true);
                AbilityFeedback.play(player, "block.chest.open");
                AbilityFeedback.particles(player, ParticleTypes.END_ROD, 8, 1.6D, 0.45D);
            }
        });
        context.setPacketHandled(true);
    }
}
