
package net.coms.abilitysgorwmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.coms.abilitysgorwmod.animation.AnimationClip;
import net.coms.abilitysgorwmod.animation.AnimationPlayer;
import net.coms.abilitysgorwmod.animation.PlayerAnimationHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonItemInHandRendererMixin {

    @Unique
    private boolean abilitysgorwmod$pushedPose;

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void abilitysgorwmod$beforeItem(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        abilitysgorwmod$pushedPose = false;

        if (player != Minecraft.getInstance().player) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();

        AnimationPlayer animation =
                PlayerAnimationHandler.getCurrentPlayer();

        if (animation == null || !animation.isPlaying()) {
            return;
        }

        String armName = arm == HumanoidArm.LEFT
                ? "Left Arm"
                : "Right Arm";

        AnimationClip.Vec3 all = animation.getRotation("all");
        AnimationClip.Vec3 specific = animation.getRotation(armName);

        if (all == null && specific == null) {
            return;
        }

        poseStack.pushPose();
        abilitysgorwmod$pushedPose = true;

        if (all != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) all.z));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) all.y));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) all.x));
        }

        if (specific != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) specific.z));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) specific.y));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) specific.x));
        }
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void abilitysgorwmod$afterItem(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (abilitysgorwmod$pushedPose) {
            poseStack.popPose();
            abilitysgorwmod$pushedPose = false;
        }
    }
}
