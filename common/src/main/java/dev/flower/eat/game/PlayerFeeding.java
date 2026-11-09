package dev.flower.eat.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 向空手交付一份食物，再调用接收者的原生进食流程。 Hand one serving to an empty hand, then run the recipient’s native item-use path. */
public final class PlayerFeeding {
    private PlayerFeeding() {}

    public static InteractionResult interact(Player feeder, Entity entity, InteractionHand hand) {
        if (!(entity instanceof Player target) || feeder == target) return InteractionResult.PASS;
        // 联机只认服务端设置，客户端本地配置不限制远程服务器。
        // Only the server's settings govern multiplayer interactions.
        if (!feeder.level().isClientSide && !EatConfig.allowsPlayers()) return InteractionResult.PASS;
        ItemStack offered = feeder.getItemInHand(hand);
        if (!VersionFood.playerFood(offered)) return InteractionResult.PASS;
        if (!feeder.isAlive() || !target.isAlive() || feeder.isSpectator() || target.isSpectator()
                || feeder.level() != target.level() || feeder.distanceToSqr(target) > 9D || !feeder.hasLineOfSight(target)) return InteractionResult.FAIL;
        // 其他玩家的饱食度仅由服务端持有，不向此客户端同步。 Remote players’ hunger is server state and is not synchronized to this client.
        if ((!feeder.level().isClientSide && !VersionFood.canPlayerEat(target, offered)) || target.isUsingItem()) return InteractionResult.FAIL;
        InteractionHand receivingHand = target.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND
                : target.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND : null;
        if (receivingHand == null) {
            if (!feeder.level().isClientSide) feeder.displayClientMessage(Component.translatable("eat.feeding.hands_full"), true);
            return InteractionResult.FAIL;
        }
        if (feeder.level().isClientSide) return InteractionResult.SUCCESS;
        if (feeder.tickCount < ((FedPlayer)feeder).eat$getNextFeedingTick()) return InteractionResult.FAIL;
        ItemStack serving = offered.copy(); serving.setCount(1);
        ItemStack beforeUse = serving.copy();
        ((FedPlayer)target).eat$setExternallyFed(true);
        target.setItemInHand(receivingHand, serving);
        FeedingVisuals.syncHand(target, receivingHand);
        var result = serving.use(target.level(), target, receivingHand);
        if (!result.getResult().consumesAction()) {
            target.setItemInHand(receivingHand, ItemStack.EMPTY);
            ((FedPlayer)target).eat$setExternallyFed(false);
            FeedingVisuals.syncHand(target, receivingHand);
            return InteractionResult.FAIL;
        }
        target.setItemInHand(receivingHand, result.getObject());
        if (!ItemStack.matches(beforeUse, result.getObject())) FeedingVisuals.syncHand(target, receivingHand);
        if (!feeder.getAbilities().instabuild) offered.shrink(1);
        // 仅服务端修改背包，原生使用流程处理时长、效果和容器。 Only the server changes inventory; native use handles timing, effects and remainders.
        target.getInventory().setChanged(); feeder.getInventory().setChanged();
        ((FedPlayer)feeder).eat$setNextFeedingTick((long)feeder.tickCount + 2);
        return InteractionResult.CONSUME;
    }
}
