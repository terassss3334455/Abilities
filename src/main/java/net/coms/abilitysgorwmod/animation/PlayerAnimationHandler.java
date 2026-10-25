package net.coms.abilitysgorwmod.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PlayerAnimationHandler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PlayerAnimationHandler {
    public static final String MOD_ID = "abilitysgorwmod";
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAnimationHandler.class);
    private static final ResourceLocation DEFAULT_FILE = new ResourceLocation(MOD_ID, "animations/player.animation.json");
    private static final Map<UUID, AnimationPlayer> PLAYERS = new HashMap<>();
    private PlayerAnimationHandler() {}

    public static void playAnimation(String name) { playAnimation(DEFAULT_FILE.toString(), name); }
    public static void playAnimation(String resourcePath, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) playAnimation(mc.player.getUUID(), resourcePath, name);
    }
    public static void playAnimation(UUID playerId, String resourcePath, String name) {
        ResourceLocation file = ResourceLocation.tryParse(resourcePath);
        if (file == null) { LOGGER.error("Invalid animation resource location: {}", resourcePath); return; }
        try {
            AnimationPlayer player = new AnimationPlayer(AnimationLoader.load(file, name));
            player.play();
            PLAYERS.put(playerId, player);
            LOGGER.info("Started animation '{}' from '{}' for {}", name, resourcePath, playerId);
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Failed to load animation '{}' from '{}'", name, resourcePath, ex);
        }
    }
    public static void receiveAnimation(UUID playerId, String resourcePath, String name, String expectedHash, boolean stop) {
        if (stop) { stopAnimation(playerId); return; }
        String actualHash=net.coms.abilitysgorwmod.client.AnimationResourceHash.sha256(resourcePath);
        String resolvedResource=resourcePath;
        if(actualHash==null||!actualHash.equals(expectedHash)){
            resolvedResource=net.coms.abilitysgorwmod.client.AnimationResourceHash.findCustomResourceByHash(expectedHash);
            if(resolvedResource==null){LOGGER.error("Animation data missing or hash mismatch for {} ({})",playerId,resourcePath);return;}
        }
        playAnimation(playerId, resolvedResource, name);
    }
    public static void stopAnimation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) stopAnimation(mc.player.getUUID());
    }
    public static void stopAnimation(UUID id) {
        AnimationPlayer old = PLAYERS.remove(id);
        if (old != null) old.stop();
    }
    private static AnimationPlayer forEntity(LivingEntity entity) {
        return entity instanceof Player ? PLAYERS.get(entity.getUUID()) : null;
    }
    public static AnimationPlayer getCurrentPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PLAYERS.get(mc.player.getUUID());
    }
    public static boolean isAnimationPlaying() {
        AnimationPlayer p = getCurrentPlayer(); return p != null && p.isPlaying();
    }

    public static void applyAnimation(HumanoidModel<?> model, LivingEntity entity) {
        AnimationPlayer player = forEntity(entity);
        if (player == null || !player.isPlaying()) return;
        for (String bone : player.getClip().rotations.keySet()) {
            if (bone.equalsIgnoreCase("all")) continue;
            AnimationClip.Vec3 r = player.getRotation(bone);
            float x=(float)Math.toRadians(r.x), y=(float)Math.toRadians(r.y), z=(float)Math.toRadians(r.z);
            switch (bone.toLowerCase(java.util.Locale.ROOT)) {
                case "head" -> rotate(model.head,x,y,z);
                case "body" -> rotate(model.body,x,y,z);
                case "left arm", "leftarm", "left_arm" -> rotate(model.leftArm,x,y,z);
                case "right arm", "rightarm", "right_arm" -> rotate(model.rightArm,x,y,z);
                case "left leg", "leftleg", "left_leg" -> rotate(model.leftLeg,x,y,z);
                case "right leg", "rightleg", "right_leg" -> rotate(model.rightLeg,x,y,z);
                default -> LOGGER.debug("Unsupported humanoid bone: {}", bone);
            }
        }
    }
    private static void rotate(ModelPart part,float x,float y,float z) { part.xRot+=x; part.yRot+=y; part.zRot+=z; }

    public static boolean pushThirdPersonRootTransform(PoseStack pose, LivingEntity entity) {
        AnimationPlayer p=forEntity(entity);
        if(p==null||!p.isPlaying()) return false;
        AnimationClip clip=p.getClip();
        boolean hasR=clip.rotations.keySet().stream().anyMatch("all"::equalsIgnoreCase);
        boolean hasP=clip.positions.keySet().stream().anyMatch("all"::equalsIgnoreCase);
        if(!hasR&&!hasP) return false;
        AnimationClip.Vec3 r=all(p,true), v=all(p,false);
        pose.pushPose(); pose.translate(v.x/16.0,v.y/16.0,v.z/16.0);
        pose.mulPose(Axis.ZP.rotationDegrees((float)r.z)); pose.mulPose(Axis.YP.rotationDegrees((float)r.y)); pose.mulPose(Axis.XP.rotationDegrees((float)r.x));
        return true;
    }
    private static AnimationClip.Vec3 all(AnimationPlayer p,boolean rotation) {
        Map<String,?> map=rotation?p.getClip().rotations:p.getClip().positions;
        for(String key:map.keySet()) if(key.equalsIgnoreCase("all")) return rotation?p.getRotation(key):p.getPosition(key);
        return new AnimationClip.Vec3(0,0,0);
    }
    public static AnimationClip.Vec3 getFirstPersonRotation(HumanoidArm arm) {
        AnimationPlayer p=getCurrentPlayer(); if(p==null||!p.isPlaying()) return null;
        String bone=arm==HumanoidArm.LEFT?"Left Arm":"Right Arm";
        AnimationClip.Vec3 a=all(p,true), b=p.getRotation(bone);
        boolean hasAll=p.getClip().rotations.keySet().stream().anyMatch("all"::equalsIgnoreCase);
        if(!hasAll) return b; if(b==null) return a; return new AnimationClip.Vec3(a.x+b.x,a.y+b.y,a.z+b.z);
    }
    public static void applyFirstPersonArm(PoseStack pose,HumanoidArm arm) {
        AnimationClip.Vec3 r=getFirstPersonRotation(arm); if(r==null)return;
        pose.mulPose(Axis.ZP.rotationDegrees((float)r.z)); pose.mulPose(Axis.YP.rotationDegrees((float)r.y)); pose.mulPose(Axis.XP.rotationDegrees((float)r.x));
    }
    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){PLAYERS.clear();return;}
        PLAYERS.entrySet().removeIf(entry->{AnimationPlayer p=entry.getValue(); if(p.isPlaying())p.tick(1.0F/20.0F); return !p.isPlaying() || mc.level.getPlayerByUUID(entry.getKey()) == null;});
    }
}