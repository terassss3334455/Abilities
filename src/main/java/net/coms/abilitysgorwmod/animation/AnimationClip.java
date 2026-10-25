package net.coms.abilitysgorwmod.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class AnimationClip {

    public static final class Vec3 {
        public final double x;
        public final double y;
        public final double z;

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static Vec3 lerp(Vec3 a, Vec3 b, float t) {
            return new Vec3(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t,
                    a.z + (b.z - a.z) * t
            );
        }
    }

    public final String name;
    public final float length;
    public final boolean loop;

    public final Map<String, NavigableMap<Float, Vec3>> rotations;
    public final Map<String, NavigableMap<Float, Vec3>> positions;

    public AnimationClip(
            String name,
            float length,
            boolean loop,
            Map<String, NavigableMap<Float, Vec3>> rotations
    ) {
        this(name, length, loop, rotations, Collections.emptyMap());
    }

    public AnimationClip(
            String name,
            float length,
            boolean loop,
            Map<String, NavigableMap<Float, Vec3>> rotations,
            Map<String, NavigableMap<Float, Vec3>> positions
    ) {
        this.name = name;
        this.length = Math.max(0.0F, length);
        this.loop = loop;
        this.rotations = immutableCopy(rotations);
        this.positions = immutableCopy(positions);
    }

    private static Map<String, NavigableMap<Float, Vec3>> immutableCopy(
            Map<String, NavigableMap<Float, Vec3>> source
    ) {
        Map<String, NavigableMap<Float, Vec3>> copy = new HashMap<>();

        source.forEach((bone, keys) ->
                copy.put(
                        bone,
                        Collections.unmodifiableNavigableMap(
                                new TreeMap<>(keys)
                        )
                )
        );

        return Collections.unmodifiableMap(copy);
    }

    public String getName() {
        return name;
    }

    public float getLength() {
        return length;
    }

    public boolean isLooping() {
        return loop;
    }

    public Set<String> getBoneNames() {
        java.util.Set<String> names = new java.util.HashSet<>();
        names.addAll(rotations.keySet());
        names.addAll(positions.keySet());
        return Collections.unmodifiableSet(names);
    }

    /**
     * 旧コードとの互換性を維持する。
     */
    public Set<String> getBiomeNames() {
        return getBoneNames();
    }

    public Vec3 getRotation(String bone, float seconds) {
        return getValue(rotations, bone, seconds);
    }

    public Vec3 getPosition(String bone, float seconds) {
        return getValue(positions, bone, seconds);
    }

    private Vec3 getValue(
            Map<String, NavigableMap<Float, Vec3>> source,
            String bone,
            float seconds
    ) {
        NavigableMap<Float, Vec3> keys = findKeys(source, bone);

        if (keys == null || keys.isEmpty()) {
            return new Vec3(0, 0, 0);
        }

        Map.Entry<Float, Vec3> before = keys.floorEntry(seconds);
        Map.Entry<Float, Vec3> after = keys.ceilingEntry(seconds);

        if (before == null) {
            return keys.firstEntry().getValue();
        }

        if (after == null) {
            return keys.lastEntry().getValue();
        }

        if (before.getKey().equals(after.getKey())) {
            return before.getValue();
        }

        float span = after.getKey() - before.getKey();

        if (span <= 0.0F) {
            return after.getValue();
        }

        float progress = (seconds - before.getKey()) / span;

        progress = Math.max(0.0F, Math.min(1.0F, progress));

        return Vec3.lerp(
                before.getValue(),
                after.getValue(),
                progress
        );
    }

    private NavigableMap<Float, Vec3> findKeys(
            Map<String, NavigableMap<Float, Vec3>> source,
            String bone
    ) {
        NavigableMap<Float, Vec3> exact = source.get(bone);

        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, NavigableMap<Float, Vec3>> entry
                : source.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(bone)) {
                return entry.getValue();
            }
        }

        return null;
    }
}