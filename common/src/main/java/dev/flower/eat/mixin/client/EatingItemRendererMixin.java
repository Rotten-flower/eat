package dev.flower.eat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flower.eat.game.EatingItem;
import dev.flower.eat.game.EatingSession;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class EatingItemRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void eat$shrinkServing(ItemEntity item, float yaw, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        long start = ((EatingItem)item).eat$getStart();
        if (start < 0) return;
        float progress = (item.level().getGameTime() - start + partialTick) / EatingSession.DURATION;
        float scale = Math.max(.12F, 1F - Math.max(0F, progress) * .88F);
        pose.scale(scale, scale, scale);
    }
}
