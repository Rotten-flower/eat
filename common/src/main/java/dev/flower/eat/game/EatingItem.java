package dev.flower.eat.game;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public interface EatingItem {
    long eat$getStart();
    int eat$getEaterId();
    void eat$setTimeline(long start, int eaterId);
    void eat$reserve(Mob mob, Vec3 returnPosition);
    void eat$release(boolean returnToGround);
}
