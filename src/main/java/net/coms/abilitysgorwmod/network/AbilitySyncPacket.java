package net.coms.abilitysgorwmod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilitySyncPacket {

    private final CompoundTag data;

    public AbilitySyncPacket(CompoundTag data) {
        this.data = data.copy();
    }

    public static void encode(
            AbilitySyncPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeNbt(packet.data);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();

        return new AbilitySyncPacket(
                data == null ? new CompoundTag() : data
        );
    }

    public static void handle(
            AbilitySyncPacket packet,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() ->
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () ->
                                net.coms.abilitysgorwmod.client
                                        .AbilityClientSync.apply(packet.data)
                )
        );

        context.setPacketHandled(true);
    }
}