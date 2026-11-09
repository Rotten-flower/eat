package dev.flower.eat.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.ai.goal.GoalSelector;

@Mixin(Mob.class)
public interface MobFeedInvoker {
    @Invoker("mobInteract") InteractionResult eat$feed(Player player, InteractionHand hand);
    @Accessor("goalSelector") GoalSelector eat$getGoalSelector();
}
