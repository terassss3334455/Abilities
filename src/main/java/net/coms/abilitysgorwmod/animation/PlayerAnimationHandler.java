package net.coms.abilitysgorwmod.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = PlayerAnimationHandler.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class PlayerAnimationHandler {

    public static final String MOD_ID = "abilitysgorwmod";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerAnimationHandler.class);

    private static final ResourceLocation DEFAULT_ANIMATION_FILE =
            new ResourceLocation(
                    MOD_ID,
                    "animations/player.animation.json"
            );

    private static AnimationPlayer currentPlayer;

    private PlayerAnimationHandler() {
    }

    public static void playAnimation(String animationName) {
        playAnimation(
                DEFAULT_ANIMATION_FILE.toString(),
                animationName
        );
    }

    public static void playAnimation(
            String resourcePath,
            String animationName
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        ResourceLocation file =
                ResourceLocation.tryParse(resourcePath);

        if (file == null) {
            LOGGER.error(
                    "Invalid animation resource location: {}",
                    resourcePath
            );
            return;
        }

        try {
            AnimationClip clip =
                    AnimationLoader.load(file, animationName);

            AnimationPlayer newPlayer =
                    new AnimationPlayer(clip);

            newPlayer.play();
            currentPlayer = newPlayer;

            LOGGER.info(
                    "Started animation '{}' from '{}'",
                    animationName,
                    resourcePath
            );

        } catch (IOException | RuntimeException exception) {
            LOGGER.error(
                    "Failed to load animation '{}' from '{}'",
                    animationName,
                    resourcePath,
                    exception
            );
        }
    }

    public static void stopAnimation() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer = null;
        }
    }

    public static AnimationPlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public static boolean isAnimationPlaying() {
        return currentPlayer != null
                && currentPlayer.isPlaying();
    }

    public static void applyAnimation(
            HumanoidModel<?> model,
            LivingEntity entity
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || entity != minecraft.player
                || currentPlayer == null
                || !currentPlayer.isPlaying()) {
            return;
        }

        AnimationClip clip = currentPlayer.getClip();

        for (Map.Entry<String, ?> entry : clip.rotations.entrySet()) {
            String boneName = entry.getKey();

            if (isAllBone(boneName)) {
                continue;
            }

            AnimationClip.Vec3 rotation =
                    currentPlayer.getRotation(boneName);

            applyBoneRotation(model, boneName, rotation);
        }
    }

    private static boolean isAllBone(String boneName) {
        return "all".equalsIgnoreCase(boneName);
    }

    private static void applyBoneRotation(
            HumanoidModel<?> model,
            String boneName,
            AnimationClip.Vec3 rotation
    ) {
        float x = (float) Math.toRadians(rotation.x);
        float y = (float) Math.toRadians(rotation.y);
        float z = (float) Math.toRadians(rotation.z);

        switch (boneName.toLowerCase(java.util.Locale.ROOT)) {
            case "head":
                rotatePart(model.head, x, y, z);
                break;

            case "body":
                rotatePart(model.body, x, y, z);
                break;

            case "left arm":
            case "leftarm":
            case "left_arm":
                rotatePart(model.leftArm, x, y, z);
                break;

            case "right arm":
            case "rightarm":
            case "right_arm":
                rotatePart(model.rightArm, x, y, z);
                break;

            case "left leg":
            case "leftleg":
            case "left_leg":
                rotatePart(model.leftLeg, x, y, z);
                break;

            case "right leg":
            case "rightleg":
            case "right_leg":
                rotatePart(model.rightLeg, x, y, z);
                break;

            default:
                LOGGER.debug(
                        "Unsupported humanoid bone: {}",
                        boneName
                );
                break;
        }
    }

    private static void rotatePart(
            ModelPart part,
            float x,
            float y,
            float z
    ) {
        part.xRot += x;
        part.yRot += y;
        part.zRot += z;
    }

    public static boolean pushThirdPersonRootTransform(
            PoseStack poseStack,
            LivingEntity entity
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || entity != minecraft.player
                || currentPlayer == null
                || !currentPlayer.isPlaying()) {
            return false;
        }

        AnimationClip clip = currentPlayer.getClip();

        boolean hasRotation = containsBone(clip.rotations, "all");
        boolean hasPosition = containsBone(clip.positions, "all");

        if (!hasRotation && !hasPosition) {
            return false;
        }

        AnimationClip.Vec3 position = getAllPosition();
        AnimationClip.Vec3 rotation = getAllRotation();

        poseStack.pushPose();

        poseStack.translate(
                position.x / 16.0,
                position.y / 16.0,
                position.z / 16.0
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees((float) rotation.z)
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees((float) rotation.y)
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees((float) rotation.x)
        );

        return true;
    }

    private static boolean containsBone(
            Map<String, ?> map,
            String name
    ) {
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static AnimationClip.Vec3 getAllRotation() {
        if (currentPlayer == null) {
            return new AnimationClip.Vec3(0, 0, 0);
        }

        AnimationClip clip = currentPlayer.getClip();

        for (String key : clip.rotations.keySet()) {
            if (isAllBone(key)) {
                return currentPlayer.getRotation(key);
            }
        }

        return new AnimationClip.Vec3(0, 0, 0);
    }

    private static AnimationClip.Vec3 getAllPosition() {
        if (currentPlayer == null) {
            return new AnimationClip.Vec3(0, 0, 0);
        }

        AnimationClip clip = currentPlayer.getClip();

        for (String key : clip.positions.keySet()) {
            if (isAllBone(key)) {
                return currentPlayer.getPosition(key);
            }
        }

        return new AnimationClip.Vec3(0, 0, 0);
    }

    public static AnimationClip.Vec3 getFirstPersonRotation(
            HumanoidArm arm
    ) {
        if (currentPlayer == null
                || !currentPlayer.isPlaying()) {
            return null;
        }

        String armName = arm == HumanoidArm.LEFT
                ? "Left Arm"
                : "Right Arm";

        AnimationClip.Vec3 all = getAllRotation();

        AnimationClip.Vec3 specific = null;

        AnimationClip clip = currentPlayer.getClip();

        for (String key : clip.rotations.keySet()) {
            if (key.equalsIgnoreCase(armName)) {
                specific = currentPlayer.getRotation(key);
                break;
            }
        }

        boolean hasAll = containsBone(clip.rotations, "all");

        if (!hasAll) {
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

    public static void applyFirstPersonArm(
            PoseStack poseStack,
            HumanoidArm arm
    ) {
        AnimationClip.Vec3 rotation =
                getFirstPersonRotation(arm);

        if (rotation == null) {
            return;
        }

        poseStack.mulPose(
                Axis.ZP.rotationDegrees((float) rotation.z)
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees((float) rotation.y)
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees((float) rotation.x)
        );
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            currentPlayer = null;
            return;
        }

        if (currentPlayer != null && currentPlayer.isPlaying()) {
            currentPlayer.tick(1.0F / 20.0F);
        }
    }
}