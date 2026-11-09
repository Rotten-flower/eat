package dev.flower.eat.game;

public interface FedPlayer {
    boolean eat$isExternallyFed();
    void eat$setExternallyFed(boolean fed);
    long eat$getNextFeedingTick();
    void eat$setNextFeedingTick(long tick);
}
