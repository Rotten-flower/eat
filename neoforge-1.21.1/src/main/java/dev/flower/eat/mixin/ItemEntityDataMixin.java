package dev.flower.eat.mixin;

import dev.flower.eat.game.EatingItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityDataMixin implements EatingItem {
    @Unique private static final EntityDataAccessor<Long> eat$START = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.LONG);
    @Unique private static final EntityDataAccessor<Integer> eat$EATER = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void eat$defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(eat$START, -1L); builder.define(eat$EATER, -1);
    }
    @Override public long eat$getStart() { return ((ItemEntity)(Object)this).getEntityData().get(eat$START); }
    @Override public int eat$getEaterId() { return ((ItemEntity)(Object)this).getEntityData().get(eat$EATER); }
    @Override public void eat$setTimeline(long start, int eaterId) {
        ((ItemEntity)(Object)this).getEntityData().set(eat$START, start);
        ((ItemEntity)(Object)this).getEntityData().set(eat$EATER, eaterId);
    }
}
