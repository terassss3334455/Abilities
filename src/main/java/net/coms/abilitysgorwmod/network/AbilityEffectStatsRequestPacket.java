package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.function.Supplier;

/** Requests server-authoritative chance values, including currently equipped item modifiers. */
public final class AbilityEffectStatsRequestPacket {
    public static void encode(AbilityEffectStatsRequestPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {}
    public static AbilityEffectStatsRequestPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new AbilityEffectStatsRequestPacket();
    }

    public static void handle(AbilityEffectStatsRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ListTag chances = new ListTag();
            for (Map.Entry<String, Float> entry : AbilityApi.getRegisteredAbilityChances().entrySet()) {
                String abilityId = entry.getKey();
                if (!PlayerAbilityData.hasAbility(player, abilityId)) continue;

                float baseChance = entry.getValue();
                CompoundTag row = new CompoundTag();
                row.putString("id", abilityId);
                row.putFloat("base", baseChance);
                row.putFloat("effective", AbilityApi.getAbilityChance(player, abilityId, baseChance));
                row.putBoolean("active", AbilityApi.isAbilityEnabled(player, abilityId));
                chances.add(row);
            }

            CompoundTag data = new CompoundTag();
            data.put("chances", chances);
            AbilityNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new AbilityEffectStatsSyncPacket(data)
            );
        });
        context.setPacketHandled(true);
    }
}