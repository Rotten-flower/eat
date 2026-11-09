package dev.flower.eat.mixin;

import dev.flower.eat.game.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 保留原生物品目标 AI，仅延后即时消耗以完成咀嚼。 Keep native item targeting; delay immediate consumption until chewing finishes. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems", remap = false)
public abstract class CapuchinPickupMixin extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
    protected CapuchinPickupMixin(Mob mob, boolean sight) { super(mob, sight); }
    @Shadow(remap = false) protected ItemEntity targetEntity;
    @Inject(method = {"tick", "m_8037_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void eat$delayNativePickup(CallbackInfo ci) {
        if (!mob.getClass().getName().equals("com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey") || targetEntity == null) return;
        if (((EatingItem)targetEntity).eat$getStart() >= 0) { ci.cancel(); return; }
        if (GroundFeeding.accepts(mob, targetEntity) && ((EatingMob)mob).eat$getSession().begin(targetEntity)) ci.cancel();
    }
}
