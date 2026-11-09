package dev.flower.eat.mixin;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Doggy Talents 1.20.1 使用独立调度器并清除原版目标。 Doggy Talents 1.20.1 uses its own scheduler and clears vanilla goals. */
@Pseudo
@Mixin(targets = "doggytalents.common.entity.ai.DogAiManager", remap = false)
public interface DogGoalRegistry {
    @Invoker(value = "registerDogGoal", remap = false)
    WrappedGoal eat$registerFoodGoal(int priority, Goal goal);
}
