package net.coms.abilitysgorwmod.animation;

import net.coms.abilitysgorwmod.animation.AnimationClip;

public final class AnimationPlayer {
    private final AnimationClip clip;

    private float elapsedSeconds;
    private boolean playing;

    public AnimationPlayer(AnimationClip clip) {
        this.clip = clip;
        this.elapsedSeconds = 0.0F;
        this.playing = false;
    }

    public void play() {
        elapsedSeconds = 0.0F;
        playing = true;
    }

    public void stop() {
        playing = false;
        elapsedSeconds = 0.0F;
    }

    public void tick(float deltaSeconds) {
        if (!playing || deltaSeconds <= 0.0F) {
            return;
        }

        float length = clip.getLength();

        if (length <= 0.0F) {
            playing = false;
            return;
        }

        elapsedSeconds += deltaSeconds;

        if (clip.isLooping()) {
            elapsedSeconds %= length;
        } else if (elapsedSeconds >= length) {
            elapsedSeconds = length;
            playing = false;
        }
    }

    public AnimationClip.Vec3 getRotation(String boneName) {
        return clip.getRotation(boneName, elapsedSeconds);
    }

    public AnimationClip.Vec3 getPosition(String boneName) {
        return clip.getPosition(boneName, elapsedSeconds);
    }

    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public boolean isPlaying() {
        return playing;
    }

    public AnimationClip getClip() {
        return clip;
    }
}