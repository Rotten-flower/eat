package dev.flower.eat.mixin;

import dev.flower.eat.game.EatingMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 地面觅食期间才暂停原版 Brain 的食物吸引行为。 Vanilla Brain temptation yields only while ground feeding is active. */
@Mixin(FollowTemptation.class)
public abstract class FollowTemptationMixin {
    @Inject(method = "canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z", at = @At("HEAD"), cancellable = true)
    private void eat$yieldToGroundFood(ServerLevel level, PathfinderMob mob, long time, CallbackInfoReturnable<Boolean> cir) {
        if (((EatingMob)mob).eat$isSeekingGroundFood()) cir.setReturnValue(false);
    }
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V", at = @At("HEAD"), cancellable = true)
    private void eat$keepGroundTarget(ServerLevel level, PathfinderMob mob, long time, CallbackInfo ci) {
        if (((EatingMob)mob).eat$isSeekingGroundFood()) ci.cancel();
    }
}
