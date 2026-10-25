package net.coms.abilitysgorwmod.network;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Function;
import java.util.function.Supplier;

public final class DimensionShiftPacket {
    private static final String COOLDOWN = "abilitysgorwmod_dimension_shift_cooldown";

    public static void encode(DimensionShiftPacket packet, FriendlyByteBuf buffer) {}
    public static DimensionShiftPacket decode(FriendlyByteBuf buffer) { return new DimensionShiftPacket(); }

    public static void handle(DimensionShiftPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !PlayerAbilityData.isActive(player, "dimension_shift")) return;
            int cooldown = player.getPersistentData().getInt(COOLDOWN);
            if (cooldown > 0) {
                player.displayClientMessage(Component.literal("境界渡りはまだ使用できません。"), true);
                return;
            }

            ServerLevel destination;
            if (player.level().dimension() == Level.NETHER) {
                destination = player.server.getLevel(Level.OVERWORLD);
            } else if (player.level().dimension() == Level.OVERWORLD) {
                destination = player.server.getLevel(Level.NETHER);
            } else {
                player.displayClientMessage(Component.literal("現世とネザーでのみ使用できます。"), true);
                return;
            }
            if (destination == null) return;

            double[] safe = findSafe(destination, player.getX(), player.getY(), player.getZ());
            if (safe == null) {
                player.displayClientMessage(Component.literal("安全な転移先が見つかりません。"), true);
                return;
            }
            final double x = safe[0], y = safe[1], z = safe[2];
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            Entity changed = player.changeDimension(destination, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                                          float rotationYaw, Function<Boolean, Entity> repositionEntity) {
                    Entity placed = repositionEntity.apply(false);
                    if (placed != null) placed.moveTo(x, y, z, yaw, pitch);
                    return placed;
                }
            });
            if (changed != null) {
                changed.getPersistentData().putInt(COOLDOWN, 100);
                if (changed instanceof ServerPlayer changedPlayer) {
                    // Refresh client-side ability state immediately after transfer.
                    AbilityNetwork.syncPlayer(changedPlayer);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean isHazard(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static double[] findSafe(ServerLevel level, double x, double y, double z) {
        int baseY = Math.max(level.getMinBuildHeight() + 1,
                Math.min(level.getMaxBuildHeight() - 3, (int) Math.floor(y)));
        for (int radius = 0; radius <= 16; radius++) {
            for (int angle = 0; angle < 360; angle += (radius == 0 ? 360 : 15)) {
                double tx = x + Math.cos(Math.toRadians(angle)) * radius;
                double tz = z + Math.sin(Math.toRadians(angle)) * radius;
                for (int dy = 0; dy <= 12; dy++) {
                    for (int sign : new int[]{1, -1}) {
                        int ty = baseY + dy * sign;
                        if (ty <= level.getMinBuildHeight() || ty >= level.getMaxBuildHeight() - 2) continue;
                        BlockPos feet = BlockPos.containing(tx, ty, tz);
                        BlockState feetState = level.getBlockState(feet);
                        BlockState headState = level.getBlockState(feet.above());
                        BlockState floorState = level.getBlockState(feet.below());
                        if (!feetState.getCollisionShape(level, feet).isEmpty()
                                || !headState.getCollisionShape(level, feet.above()).isEmpty()
                                || !level.getFluidState(feet).isEmpty()
                                || !level.getFluidState(feet.above()).isEmpty()
                                || !floorState.isFaceSturdy(level, feet.below(), Direction.UP)
                                || isHazard(feetState) || isHazard(headState) || isHazard(floorState)) continue;
                        return new double[]{tx, ty, tz};
                    }
                }
            }
        }
        return null;
    }
}
