package dev.flower.eat.mixin;

import dev.flower.eat.game.FedPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerFeedingMixin implements FedPlayer {
    @Unique private long eat$nextFeedingTick;
    @Override public long eat$getNextFeedingTick() { return eat$nextFeedingTick; }
    @Override public void eat$setNextFeedingTick(long tick) { eat$nextFeedingTick = tick; }
    @Inject(method = "tick", at = @At("TAIL"))
    private void eat$finishExternalMeal(CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (!player.level().isClientSide && (!player.isUsingItem() || !dev.flower.eat.game.VersionFood.playerFood(player.getUseItem()))
                && ((FedPlayer)player).eat$isExternallyFed())
            ((FedPlayer)player).eat$setExternallyFed(false);
    }
}
