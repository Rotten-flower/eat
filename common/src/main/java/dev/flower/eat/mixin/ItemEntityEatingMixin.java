package dev.flower.eat.mixin;

import dev.flower.eat.game.EatingItem;
import dev.flower.eat.game.EatingSession;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityEatingMixin implements EatingItem {
    @Unique private Vec3 eat$returnPosition;
    @Unique private boolean eat$originalNoGravity;

    @Override public void eat$reserve(Mob mob, Vec3 returnPosition) {
        ItemEntity item = (ItemEntity)(Object)this;
        eat$returnPosition = returnPosition;
        eat$originalNoGravity = item.isNoGravity();
        eat$setTimeline(item.level().getGameTime(), mob.getId());
    }

    @Override public void eat$release(boolean returnToGround) {
        ItemEntity item = (ItemEntity)(Object)this;
        eat$setTimeline(-1L, -1);
        item.setNoPickUpDelay(); item.setNoGravity(eat$originalNoGravity);
        if (returnToGround && eat$returnPosition != null) item.setPos(eat$returnPosition);
        item.setDeltaMovement(Vec3.ZERO); item.hasImpulse = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void eat$releaseOrphan(CallbackInfo ci) {
        ItemEntity item = (ItemEntity)(Object)this;
        if (eat$getStart() < 0 || !(item.level() instanceof ServerLevel level)) return;
        var eater = level.getEntity(eat$getEaterId());
        if (!(eater instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()
                || level.getGameTime() > eat$getStart() + EatingSession.DURATION + 2) eat$release(true);
    }

    @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
    private void eat$keepServingSeparate(CallbackInfoReturnable<Boolean> cir) {
        if (eat$getStart() >= 0) cir.setReturnValue(false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void eat$saveOnGround(CompoundTag tag, CallbackInfo ci) {
        if (eat$getStart() < 0 || eat$returnPosition == null) return;
        // 进食中途保存也写入普通掉落物，重载后可正常拾取。 Save an ordinary recoverable item even during an unfinished bite.
        tag.putShort("PickupDelay", (short)0); tag.putBoolean("NoGravity", eat$originalNoGravity);
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(eat$returnPosition.x)); position.add(DoubleTag.valueOf(eat$returnPosition.y));
        position.add(DoubleTag.valueOf(eat$returnPosition.z)); tag.put("Pos", position);
    }
}
