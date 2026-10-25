package net.coms.abilitysgorwmod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server response containing the effective chances for the player's owned abilities. */
public final class AbilityEffectStatsSyncPacket {
    private final CompoundTag data;

    public AbilityEffectStatsSyncPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(AbilityEffectStatsSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static AbilityEffectStatsSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();
        return new AbilityEffectStatsSyncPacket(data);
    }

    public static void handle(AbilityEffectStatsSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.coms.abilitysgorwmod.client.AbilityEffectStatsClientData.apply(packet.data)
        ));
        context.setPacketHandled(true);
    }
}