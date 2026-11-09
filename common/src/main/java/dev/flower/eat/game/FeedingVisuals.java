package dev.flower.eat.game;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** 先同步手持物，再同步原生使用物品标记。 Sync held equipment before the native using-item flag. */
public final class FeedingVisuals {
    private FeedingVisuals() {}
    public static void syncHand(LivingEntity target, InteractionHand hand) {
        if (!(target.level() instanceof ServerLevel level)) return;
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        level.getChunkSource().broadcastAndSend(target, new ClientboundSetEquipmentPacket(target.getId(),
                List.of(Pair.of(slot, target.getItemInHand(hand).copy()))));
    }
}
