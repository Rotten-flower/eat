package dev.flower.eat.mixin;

import dev.flower.eat.game.EatingMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 即使其他模组提高食物吸引优先级，地面觅食时仍会让步。 Native temptation yields during ground feeding even at a higher priority. */
@Mixin(TemptGoal.class)
public abstract class TemptGoalMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void eat$yieldToReachableFood(CallbackInfoReturnable<Boolean> cir) {
        if (((EatingMob)mob).eat$prefersGroundFood()) cir.setReturnValue(false);
    }
}
