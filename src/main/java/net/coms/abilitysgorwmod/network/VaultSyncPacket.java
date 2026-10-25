package net.coms.abilitysgorwmod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class VaultSyncPacket {
    private final CompoundTag data;
    private final boolean openScreen;

    public VaultSyncPacket(CompoundTag data, boolean openScreen) {
        this.data = data.copy();
        this.openScreen = openScreen;
    }

    public static void encode(VaultSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
        buffer.writeBoolean(packet.openScreen);
    }

    public static VaultSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();
        return new VaultSyncPacket(data == null ? new CompoundTag() : data, buffer.readBoolean());
    }

    public static void handle(VaultSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.coms.abilitysgorwmod.client.VaultClientSync.apply(packet.data, packet.openScreen)));
        context.setPacketHandled(true);
    }
}
