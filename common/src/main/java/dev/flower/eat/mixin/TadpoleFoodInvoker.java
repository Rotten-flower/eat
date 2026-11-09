package dev.flower.eat.mixin;

import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Tadpole.class)
public interface TadpoleFoodInvoker {
    @Invoker("isFood") boolean eat$isFood(ItemStack stack);
}
