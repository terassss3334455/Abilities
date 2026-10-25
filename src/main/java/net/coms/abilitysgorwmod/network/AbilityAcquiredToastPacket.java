package net.coms.abilitysgorwmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-side toast shown only when a player acquires an ability for the first time. */
public final class AbilityAcquiredToastPacket {
    private final String abilityName;
    private final String rarityName;

    public AbilityAcquiredToastPacket(String abilityName, String rarityName) {
        this.abilityName = abilityName == null ? "能力" : abilityName;
        this.rarityName = rarityName == null ? "COMMON" : rarityName;
    }

    public static void encode(AbilityAcquiredToastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.abilityName, 256);
        buffer.writeUtf(packet.rarityName, 256);
    }

    public static AbilityAcquiredToastPacket decode(FriendlyByteBuf buffer) {
        return new AbilityAcquiredToastPacket(buffer.readUtf(256), buffer.readUtf(256));
    }

    public static void handle(AbilityAcquiredToastPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.coms.abilitysgorwmod.client.AbilityToastClientHandler.show(
                        packet.abilityName, packet.rarityName)));
        context.setPacketHandled(true);
    }
}
