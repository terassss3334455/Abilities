package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.ability.ChargeAbility;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.pack.AbilityTriggerRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AbilityCooldownSyncPacket {
    private record Row(int remaining, int total, boolean alwaysVisible) {}

    private final CompoundTag data;

    public AbilityCooldownSyncPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static AbilityCooldownSyncPacket forPlayer(ServerPlayer player) {
        Map<String, Row> rows = new LinkedHashMap<>();
        addBuiltin(rows, player, "charge", ChargeAbility.COOLDOWN_KEY,
                ChargeAbility.COOLDOWN_TICKS, AbilityApi.isAbilityEnabled(player, "charge"));
        addBuiltin(rows, player, "dimension_shift", "abilitysgorwmod_dimension_shift_cooldown",
                100, false);
        addBuiltin(rows, player, "projectile_phase", "abilitysgorwmod_projectile_phase_cooldown",
                10, false);

        for (AbilityTriggerRules.CooldownState cooldown : AbilityTriggerRules.getActiveCooldowns(player)) {
            merge(rows, cooldown.abilityId(), cooldown.remainingTicks(), cooldown.totalTicks(), false);
        }

        CompoundTag result = new CompoundTag();
        ListTag list = new ListTag();
        rows.forEach((id, row) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putInt("remaining", row.remaining());
            entry.putInt("total", row.total());
            entry.putBoolean("always", row.alwaysVisible());
            list.add(entry);
        });
        result.put("rows", list);
        return new AbilityCooldownSyncPacket(result);
    }

    private static void addBuiltin(Map<String, Row> rows, ServerPlayer player, String abilityId,
                                   String cooldownKey, int total, boolean alwaysVisible) {
        if (!PlayerAbilityData.hasAbility(player, abilityId)) return;
        int remaining = Math.max(0, player.getPersistentData().getInt(cooldownKey));
        if (remaining > 0 || alwaysVisible) merge(rows, abilityId, remaining, total, alwaysVisible);
    }

    private static void merge(Map<String, Row> rows, String abilityId, int remaining,
                              int total, boolean alwaysVisible) {
        Row previous = rows.get(abilityId);
        rows.put(abilityId, new Row(
                Math.max(remaining, previous == null ? 0 : previous.remaining()),
                Math.max(total, previous == null ? 0 : previous.total()),
                alwaysVisible || previous != null && previous.alwaysVisible()
        ));
    }

    public static void encode(AbilityCooldownSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static AbilityCooldownSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();
        return new AbilityCooldownSyncPacket(data);
    }

    public static void handle(AbilityCooldownSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.coms.abilitysgorwmod.client.AbilityCooldownHudData.apply(packet.data)));
        context.setPacketHandled(true);
    }

    public static void send(ServerPlayer player) {
        AbilityNetworkHolder.send(player, forPlayer(player));
    }

    private static final class AbilityNetworkHolder {
        private static void send(ServerPlayer player, AbilityCooldownSyncPacket packet) {
            net.coms.abilitysgorwmod.network.AbilityNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}