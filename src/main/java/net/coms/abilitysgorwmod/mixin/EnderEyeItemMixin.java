package net.coms.abilitysgorwmod.mixin;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.ability.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {

    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"
            )
    )
    private void abilitysgorwmod$preserveEyeOnThrow(
            ItemStack stack,
            int amount,
            Level level,
            Player player,
            InteractionHand hand
    ) {
        if (PlayerAbilityData.isActive(player, "ender_eye_safe")) {
            if (level instanceof ServerLevel serverLevel) {

                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                AbilityFeedback.spawn(serverLevel, ParticleTypes.END_ROD,
                        player.getX() + look.x * 0.8D,
                        player.getEyeY() - 0.2D + look.y * 0.8D,
                        player.getZ() + look.z * 0.8D,
                        8, 0.2D, 0.25D, 0.2D, 0.01D);
            }
            return;
        }
        stack.shrink(amount);
    }
}
