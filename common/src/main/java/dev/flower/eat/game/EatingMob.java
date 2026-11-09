package dev.flower.eat.game;

public interface EatingMob {
    boolean eat$prefersGroundFood();
    EatingSession eat$getSession();
    boolean eat$isChewing();
    boolean eat$isSeekingGroundFood();
    void eat$setSeekingGroundFood(boolean seeking);
}
