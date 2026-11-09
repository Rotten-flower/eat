package dev.flower.eat.mixin;

import dev.flower.eat.game.FedPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerFeedingDataMixin implements FedPlayer {
    @Unique private static final EntityDataAccessor<Boolean> EAT_FED = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void eat$defineFeedingFlag(SynchedEntityData.Builder builder, CallbackInfo ci) { builder.define(EAT_FED, false); }
    @Override public boolean eat$isExternallyFed() { return ((Player)(Object)this).getEntityData().get(EAT_FED); }
    @Override public void eat$setExternallyFed(boolean fed) { ((Player)(Object)this).getEntityData().set(EAT_FED, fed); }
}
