package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class AbilityNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(abilitysgorwmod.MODID, "main"),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static int packetId = 0;

    private AbilityNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                AbilityTogglePacket.class,
                AbilityTogglePacket::encode,
                AbilityTogglePacket::decode,
                AbilityTogglePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                packetId++,
                AbilitySyncPacket.class,
                AbilitySyncPacket::encode,
                AbilitySyncPacket::decode,
                AbilitySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                packetId++,
                AbilityRequestSyncPacket.class,
                AbilityRequestSyncPacket::encode,
                AbilityRequestSyncPacket::decode,
                AbilityRequestSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                packetId++,
                DimensionShiftPacket.class,
                DimensionShiftPacket::encode,
                DimensionShiftPacket::decode,
                DimensionShiftPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                packetId++, OpenVaultPacket.class,
                OpenVaultPacket::encode, OpenVaultPacket::decode,
                OpenVaultPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++, VaultActionPacket.class,
                VaultActionPacket::encode, VaultActionPacket::decode,
                VaultActionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++, VaultSyncPacket.class,
                VaultSyncPacket::encode, VaultSyncPacket::decode,
                VaultSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                packetId++, AbilityAcquiredToastPacket.class,
                AbilityAcquiredToastPacket::encode, AbilityAcquiredToastPacket::decode,
                AbilityAcquiredToastPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncPlayer(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new AbilitySyncPacket(
                        net.coms.abilitysgorwmod.data.PlayerAbilityData
                                .getSyncData(player)
                )
        );
    }


    public static void syncVault(ServerPlayer player, boolean openScreen) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new VaultSyncPacket(
                        net.coms.abilitysgorwmod.data.VaultInventoryData.getSyncData(player),
                        openScreen));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}