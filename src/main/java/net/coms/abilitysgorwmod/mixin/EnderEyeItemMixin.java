package net.coms.abilitysgorwmod.mixin;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
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
            return;
        }
        stack.shrink(amount);
    }
}
