package dev.flower.eat.mixin;

import dev.flower.eat.game.GroundFoodGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "doggytalents.common.entity.Dog", remap = false)
public abstract class DogNativeGoalsMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void eat$registerNativeGoal(CallbackInfo ci) {
        Mob dog = (Mob)(Object)this;
        if (dog.level().isClientSide) return;
        try {
            Object scheduler = dog.getClass().getField("dogAi").get(dog);
            // 保留原生游泳、避火、坐下、受伤和主动动作的优先级。 Keep native float, fire avoidance, sit, injury and active action priorities.
            ((DogGoalRegistry)scheduler).eat$registerFoodGoal(6, new GroundFoodGoal(dog));
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Doggy Talents native AI API is incompatible", incompatible);
        }
    }
}
