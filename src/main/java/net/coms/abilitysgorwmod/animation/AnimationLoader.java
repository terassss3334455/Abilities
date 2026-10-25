package net.coms.abilitysgorwmod.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class AnimationLoader {

    private AnimationLoader() {
    }

    public static AnimationClip load(
            ResourceLocation file,
            String animationName
    ) throws IOException {

        Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(file)
                .orElseThrow(() ->
                        new IOException(
                                "Animation resource not found: " + file
                        )
                );

        try (Reader reader = resource.openAsReader()) {
            JsonObject root =
                    JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject animations = root.getAsJsonObject("animations");

            if (animations == null || !animations.has(animationName)) {
                throw new IOException(
                        "Animation not found: " + animationName
                );
            }

            JsonObject animation =
                    animations.getAsJsonObject(animationName);

            boolean loop = animation.has("loop")
                    && animation.get("loop").isJsonPrimitive()
                    && animation.get("loop").getAsBoolean();

            JsonObject bones = animation.getAsJsonObject("bones");

            if (bones == null) {
                throw new IOException(
                        "Animation has no bones: " + animationName
                );
            }

            Map<String, NavigableMap<Float, AnimationClip.Vec3>> rotations =
                    new HashMap<>();

            Map<String, NavigableMap<Float, AnimationClip.Vec3>> positions =
                    new HashMap<>();

            float maxTime = 0.0F;

            for (String boneName : bones.keySet()) {
                JsonElement boneElement = bones.get(boneName);

                if (!boneElement.isJsonObject()) {
                    throw new IOException(
                            "Bone must be an object: " + boneName
                    );
                }

                JsonObject bone = boneElement.getAsJsonObject();

                if (bone.has("rotation")) {
                    NavigableMap<Float, AnimationClip.Vec3> keys =
                            parseChannel(
                                    bone.get("rotation"),
                                    boneName,
                                    "rotation"
                            );

                    if (!keys.isEmpty()) {
                        rotations.put(boneName, keys);
                        maxTime = Math.max(maxTime, keys.lastKey());
                    }
                }

                if (bone.has("position")) {
                    NavigableMap<Float, AnimationClip.Vec3> keys =
                            parseChannel(
                                    bone.get("position"),
                                    boneName,
                                    "position"
                            );

                    if (!keys.isEmpty()) {
                        positions.put(boneName, keys);
                        maxTime = Math.max(maxTime, keys.lastKey());
                    }
                }
            }

            float length = animation.has("animation_length")
                    ? animation.get("animation_length").getAsFloat()
                    : maxTime;

            return new AnimationClip(
                    animationName,
                    length,
                    loop,
                    rotations,
                    positions
            );
        }
    }

    private static NavigableMap<Float, AnimationClip.Vec3> parseChannel(
            JsonElement channel,
            String boneName,
            String channelName
    ) throws IOException {

        NavigableMap<Float, AnimationClip.Vec3> keys = new TreeMap<>();

        if (channel.isJsonArray() || channel.isJsonPrimitive()) {
            keys.put(0.0F, parseVec3(channel));
            return keys;
        }

        if (!channel.isJsonObject()) {
            throw new IOException(
                    "Invalid " + channelName + " for bone " + boneName
            );
        }

        JsonObject object = channel.getAsJsonObject();

        if (object.has("pre") || object.has("post")) {
            JsonElement value = object.has("post")
                    ? object.get("post")
                    : object.get("pre");

            keys.put(0.0F, parseVec3(value));
            return keys;
        }

        for (String timeText : object.keySet()) {
            float time;

            try {
                time = Float.parseFloat(timeText);
            } catch (NumberFormatException exception) {
                throw new IOException(
                        "Invalid keyframe time: " + timeText,
                        exception
                );
            }

            if (time < 0.0F || !Float.isFinite(time)) {
                throw new IOException(
                        "Invalid keyframe time: " + timeText
                );
            }

            JsonElement value = object.get(timeText);

            if (value.isJsonObject()) {
                JsonObject key = value.getAsJsonObject();

                if (key.has("post")) {
                    value = key.get("post");
                } else if (key.has("pre")) {
                    value = key.get("pre");
                } else {
                    throw new IOException(
                            "Keyframe has no pre/post value at " + timeText
                    );
                }
            }

            keys.put(time, parseVec3(value));
        }

        return keys;
    }

    private static AnimationClip.Vec3 parseVec3(
            JsonElement element
    ) throws IOException {

        if (element.isJsonPrimitive()) {
            float value;

            try {
                value = element.getAsFloat();
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Expected a numeric vector value",
                        exception
                );
            }

            return new AnimationClip.Vec3(value, value, value);
        }

        if (!element.isJsonArray()) {
            throw new IOException("Expected a numeric array");
        }

        JsonArray array = element.getAsJsonArray();

        if (array.isEmpty() || array.size() > 3) {
            throw new IOException(
                    "Vector must contain between 1 and 3 values"
            );
        }

        try {
            double x = array.get(0).getAsDouble();
            double y = array.size() > 1
                    ? array.get(1).getAsDouble() : 0.0;
            double z = array.size() > 2
                    ? array.get(2).getAsDouble() : 0.0;

            return new AnimationClip.Vec3(x, y, z);

        } catch (RuntimeException exception) {
            throw new IOException(
                    "Invalid numeric vector value",
                    exception
            );
        }
    }
}