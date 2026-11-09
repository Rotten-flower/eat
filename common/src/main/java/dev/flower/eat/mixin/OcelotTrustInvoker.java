package dev.flower.eat.mixin;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Ocelot.class)
public interface OcelotTrustInvoker {
    @Invoker("isTrusting") boolean eat$isTrusting();
}
