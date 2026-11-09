package dev.flower.eat.mixin;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;
@Mixin(Parrot.class)
public interface ParrotFoodAccessor {
    @Accessor("TAME_FOOD") static Set<Item> eat$tameFood() { throw new AssertionError("Mixin accessor was not applied"); }
    @Accessor("POISONOUS_FOOD") static Item eat$poisonFood() { throw new AssertionError("Mixin accessor was not applied"); }
}
