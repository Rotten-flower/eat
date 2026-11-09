package dev.flower.eat.mixin;

import dev.flower.eat.game.GroundFeeding;
import dev.flower.eat.game.EatConfig;
import dev.flower.eat.game.GroundFoodGoal;
import dev.flower.eat.game.EatingMob;
import dev.flower.eat.game.EatingSession;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin implements EatingMob {
    @Shadow @Final protected GoalSelector goalSelector;
    @Unique private EatingSession eat$session;
    @Unique private boolean eat$seekingGroundFood;
    @Unique private GroundFoodGoal eat$groundGoal;
    @Override public boolean eat$prefersGroundFood() {
        if (!EatConfig.allowsGround((Mob)(Object)this)) return false;
        if (eat$isSeekingGroundFood()) return true;
        if (eat$groundGoal != null) for (var wrapped : goalSelector.getAvailableGoals())
            if (wrapped.getGoal() == eat$groundGoal) return eat$groundGoal.canUse();
        return false;
    }

    @Override public EatingSession eat$getSession() {
        if (eat$session == null) eat$session = new EatingSession((Mob)(Object)this);
        return eat$session;
    }
    @Override public boolean eat$isChewing() { return eat$session != null && eat$session.active(); }
    @Override public boolean eat$isSeekingGroundFood() { return EatConfig.allowsGround((Mob)(Object)this) && (eat$seekingGroundFood || eat$isChewing()); }
    @Override public void eat$setSeekingGroundFood(boolean seeking) { eat$seekingGroundFood = seeking; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void eat$addGoal(CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (!mob.level().isClientSide && GroundFeeding.supported(mob)) {
            eat$groundGoal = new GroundFoodGoal(mob);
            goalSelector.addGoal(mob instanceof net.minecraft.world.entity.animal.Parrot ? 1 : GroundFoodGoal.PRIORITY, eat$groundGoal);
        }
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void eat$feedSeatedPet(CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (eat$isChewing()) { eat$session.tick(); return; }
        if (!EatConfig.allowsGround(mob)) return;
        if (!(mob instanceof TamableAnimal pet) || !pet.isOrderedToSit() || !GroundFeeding.supported(mob)
                || mob.getTarget() != null || (mob.tickCount + mob.getId()) % 20 != 0) return;
        for (ItemEntity item : mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(1.2D),
                candidate -> GroundFeeding.accepts(mob, candidate))) {
            if (eat$getSession().begin(item)) break;
        }
    }
}
