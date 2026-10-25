package net.coms.abilitysgorwmod.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Client cache for server-calculated ability chance totals shown in the inventory screen. */
public final class AbilityEffectStatsClientData {
    public record ChanceRow(String abilityId, float baseChance, float effectiveChance, boolean active) {}

    private static List<ChanceRow> chances = List.of();
    private static boolean loaded;

    private AbilityEffectStatsClientData() {}

    public static void beginRequest() {
        chances = List.of();
        loaded = false;
    }

    public static void apply(CompoundTag data) {
        List<ChanceRow> result = new ArrayList<>();
        ListTag list = data.getList("chances", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            String id = row.getString("id");
            if (id.isEmpty()) continue;
            result.add(new ChanceRow(
                    id,
                    row.getFloat("base"),
                    row.getFloat("effective"),
                    row.getBoolean("active")
            ));
        }
        chances = List.copyOf(result);
        loaded = true;
    }

    public static List<ChanceRow> getChances() {
        return chances;
    }

    public static boolean isLoaded() {
        return loaded;
    }
}