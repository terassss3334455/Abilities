package net.coms.abilitysgorwmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.coms.abilitysgorwmod.animation.PlayerAnimationHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {

    @Unique
    private boolean abilitysgorwmod$rootTransformActive;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void abilitysgorwmod$beginRootTransform(
            T entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        abilitysgorwmod$rootTransformActive =
                PlayerAnimationHandler.pushThirdPersonRootTransform(
                        poseStack,
                        entity
                );
    }

    @Inject(
            method = "render",
            at = @At("RETURN")
    )
    private void abilitysgorwmod$cleanupRootTransform(
            T entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (abilitysgorwmod$rootTransformActive) {
            poseStack.popPose();
            abilitysgorwmod$rootTransformActive = false;
        }
    }
}