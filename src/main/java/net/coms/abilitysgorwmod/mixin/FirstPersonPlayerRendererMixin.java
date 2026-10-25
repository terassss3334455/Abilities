
package net.coms.abilitysgorwmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.coms.abilitysgorwmod.animation.AnimationClip;
import net.coms.abilitysgorwmod.animation.AnimationPlayer;
import net.coms.abilitysgorwmod.animation.PlayerAnimationHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FirstPersonPlayerRendererMixin {

    @Unique
    private boolean abilitysgorwmod$rightHand = true;

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void abilitysgorwmod$selectRight(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            CallbackInfo ci
    ) {
        abilitysgorwmod$rightHand = true;
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void abilitysgorwmod$selectLeft(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            CallbackInfo ci
    ) {
        abilitysgorwmod$rightHand = false;
    }

    @Unique
    private AnimationClip.Vec3 abilitysgorwmod$getRotation() {
        AnimationPlayer animation =
                PlayerAnimationHandler.getCurrentPlayer();

        if (animation == null || !animation.isPlaying()) {
            return null;
        }

        String armName = abilitysgorwmod$rightHand
                ? "Right Arm"
                : "Left Arm";

        AnimationClip.Vec3 all = animation.getRotation("all");
        AnimationClip.Vec3 specific = animation.getRotation(armName);

        if (all == null) {
            return specific;
        }

        if (specific == null) {
            return all;
        }

        return new AnimationClip.Vec3(
                all.x + specific.x,
                all.y + specific.y,
                all.z + specific.z
        );
    }

    @Unique
    private void abilitysgorwmod$rotate(
            ModelPart part,
            AnimationClip.Vec3 rotation,
            float sign
    ) {
        if (rotation == null) {
            return;
        }

        part.xRot += sign * (float) Math.toRadians(rotation.x);
        part.yRot += sign * (float) Math.toRadians(rotation.y);
        part.zRot += sign * (float) Math.toRadians(rotation.z);
    }

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void abilitysgorwmod$beforeArm(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            ModelPart arm,
            ModelPart sleeve,
            CallbackInfo ci
    ) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        abilitysgorwmod$rotate(arm, abilitysgorwmod$getRotation(), 1.0F);
    }

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void abilitysgorwmod$afterArm(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            ModelPart arm,
            ModelPart sleeve,
            CallbackInfo ci
    ) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        abilitysgorwmod$rotate(arm, abilitysgorwmod$getRotation(), -1.0F);
    }

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void abilitysgorwmod$beforeSleeve(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            ModelPart arm,
            ModelPart sleeve,
            CallbackInfo ci
    ) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        abilitysgorwmod$rotate(sleeve, abilitysgorwmod$getRotation(), 1.0F);
    }

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void abilitysgorwmod$afterSleeve(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            ModelPart arm,
            ModelPart sleeve,
            CallbackInfo ci
    ) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        abilitysgorwmod$rotate(sleeve, abilitysgorwmod$getRotation(), -1.0F);
    }
}
