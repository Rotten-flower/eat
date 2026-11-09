package dev.flower.eat.game;

import com.mojang.authlib.GameProfile;
import dev.flower.eat.mixin.MobFeedInvoker;
import dev.flower.eat.mixin.TadpoleFoodInvoker;
import dev.flower.eat.mixin.OcelotTrustInvoker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.UUID;

public final class GroundFeeding {
    private GroundFeeding() {}

    public static boolean supported(Mob mob) {
        return mob instanceof Animal || mob instanceof Dolphin || mob instanceof Tadpole || FeedingAdapters.hasAdapter(mob);
    }

    private static GameProfile profile(Mob mob, ItemEntity item) {
        if (item.getOwner() instanceof Player player) return player.getGameProfile();
        if (mob instanceof TamableAnimal pet && pet.isTame()) {
            if (pet.getOwner() instanceof Player owner) return owner.getGameProfile();
            UUID ownerId = pet.getOwnerUUID();
            if (ownerId != null) return new GameProfile(ownerId, "eat");
        }
        return null;
    }

    /** 只允许喂食分支，排除装备、挤奶与骑乘交互。 Only feeding branches are eligible; equipment, milking and riding are excluded. */
    public static boolean accepts(Mob mob, ItemEntity item) {
        if (!EatConfig.allowsGround(mob)) return false;
        if (!mob.isAlive() || mob.isNoAi() || !item.isAlive() || item.hasPickUpDelay()) return false;
        if (((EatingItem)item).eat$getStart() >= 0) return false;
        ItemStack food = item.getItem();
        if (food.isEmpty()) return false;
        boolean attributed = item.getOwner() instanceof Player;
        if (FeedingAdapters.hasAdapter(mob)) return FeedingAdapters.accepts(mob, food, item.getOwner() instanceof Player p ? p : null);
        if (mob instanceof Parrot parrot) {
            if (!parrot.isTame() && VersionFood.parrotFood(food)) return attributed;
            return VersionFood.parrotPoison(food);
        }
        if (mob instanceof Tadpole) return ((TadpoleFoodInvoker)mob).eat$isFood(food);
        if (mob instanceof Dolphin) return food.is(ItemTags.FISHES);
        if (!(mob instanceof Animal animal)) return false;
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft")) {
            if (!animal.isFood(food)) return false;
            if (mob instanceof TamableAnimal pet && !pet.isTame()) return attributed;
            if (mob instanceof TamableAnimal pet && pet.isTame() && pet.getHealth() < pet.getMaxHealth()) return true;
            return animal.isBaby() || (animal.getAge() == 0 && animal.canFallInLove());
        }
        if (mob instanceof Wolf wolf && !wolf.isTame()) {
            return attributed && !wolf.isAngry() && food.is(Items.BONE);
        }
        if (!animal.isFood(food)) return false;
        if (mob instanceof Cat cat && !cat.isTame()) return attributed;
        if (mob instanceof AbstractHorse) return true;
        if (mob instanceof Panda) return true;
        if (mob instanceof Ocelot && !((OcelotTrustInvoker)mob).eat$isTrusting() && attributed) return true;
        if (mob instanceof Wolf wolf && wolf.isTame() && wolf.getHealth() < wolf.getMaxHealth()) return true;
        if (mob instanceof Cat cat && cat.isTame() && cat.getHealth() < cat.getMaxHealth()) {
            GameProfile feeder = profile(mob, item);
            if (feeder != null && feeder.getId().equals(cat.getOwnerUUID())) return true;
        }
        return animal.isBaby() || (animal.getAge() == 0 && animal.canFallInLove());
    }

    public static boolean eat(Mob mob, ItemEntity item) {
        if (!(mob.level() instanceof ServerLevel level) || !accepts(mob, item)) return false;
        if (!mob.hasLineOfSight(item) || mob.distanceToSqr(item) > reachSquared(mob)) return false;
        FeedingPlayer player = ((FeedingWorld)level).eat$getFeedingPlayer();
        ItemStack original = item.getItem().copy();
        ItemStack serving = original.copy();
        serving.setCount(1);
        player.prepare(profile(mob, item), mob.getX(), mob.getY(), mob.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, serving);
        boolean wasTame = mob instanceof TamableAnimal pet && pet.isTame();
        boolean wasSitting = mob instanceof TamableAnimal pet && pet.isOrderedToSit();
        try {
            // 马的交互还包含骑乘和背包，直接调用独立喂食分支。 Call the horse’s feeding branch directly to avoid mounting or opening its inventory.
            if (FeedingAdapters.hasAdapter(mob)) FeedingAdapters.feed(mob, player, InteractionHand.MAIN_HAND);
            else if (mob instanceof AbstractHorse horse) horse.fedFood(player, serving);
            else ((MobFeedInvoker)mob).eat$feed(player, InteractionHand.MAIN_HAND);
            ItemStack hand = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean consumed = serving.isEmpty() || hand.isEmpty() || hand.getItem() != original.getItem();
            if (!consumed) return false;
            original.shrink(1);
            if (original.isEmpty()) item.discard();
            else item.setItem(original);
            // 原生桶装食物返还空桶，将容器留在地面。 Leave the native bucket-food remainder on the ground.
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack remainder = player.getInventory().getItem(slot);
                if (!remainder.isEmpty()) mob.spawnAtLocation(remainder.copy());
            }
            return true;
        } finally {
            // 喂食中的主人交互不应额外触发坐下命令。 Do not turn the feeding interaction into an incidental sit command.
            if (wasTame && mob instanceof TamableAnimal pet) pet.setOrderedToSit(wasSitting);
            player.getInventory().clearContent();
        }
    }

    public static double reachSquared(Mob mob) {
        double reach = Math.max(1.2D, mob.getBbWidth() * 0.5D + 0.75D);
        return reach * reach;
    }
}
