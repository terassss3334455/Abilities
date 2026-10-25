package net.coms.abilitysgorwmod.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class AbilityCooldownHudData {
    public record CooldownRow(String abilityId, int remainingTicks, int totalTicks, boolean alwaysVisible) {}

    private static List<CooldownRow> rows = List.of();

    private AbilityCooldownHudData() {}

    public static void apply(CompoundTag data) {
        List<CooldownRow> result = new ArrayList<>();
        ListTag list = data.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("id");
            if (id.isEmpty()) continue;
            result.add(new CooldownRow(id, Math.max(0, entry.getInt("remaining")),
                    Math.max(1, entry.getInt("total")), entry.getBoolean("always")));
        }
        rows = List.copyOf(result);
    }

    public static void tick() {
        List<CooldownRow> updated = new ArrayList<>();
        for (CooldownRow row : rows) {
            int remaining = Math.max(0, row.remainingTicks() - 1);
            if (remaining > 0 || row.alwaysVisible()) {
                updated.add(new CooldownRow(row.abilityId(), remaining,
                        row.totalTicks(), row.alwaysVisible()));
            }
        }
        rows = List.copyOf(updated);
    }

    public static List<CooldownRow> getRows() {
        return rows;
    }
}