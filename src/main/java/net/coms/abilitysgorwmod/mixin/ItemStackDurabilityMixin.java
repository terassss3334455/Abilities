package net.coms.abilitysgorwmod.mixin;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {

    @Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
    private void abilitysgorwmod$saveDurability(
            int amount,
            LivingEntity entity,
            Consumer<LivingEntity> onBroken,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player) || player.level().isClientSide || amount <= 0) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        boolean heldInHand = stack == player.getMainHandItem() || stack == player.getOffhandItem();
        if (!heldInHand || !stack.isDamageableItem()) {
            return;
        }

        if (PlayerAbilityData.isActive(player, "durability_saver")
                && player.getRandom().nextFloat() < 0.25F) {
            ci.cancel();
        }
    }
}
