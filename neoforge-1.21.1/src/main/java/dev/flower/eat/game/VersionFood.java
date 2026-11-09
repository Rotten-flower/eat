package dev.flower.eat.game;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
public final class VersionFood {
    private VersionFood() {}
    public static boolean parrotFood(ItemStack stack) { return stack.is(ItemTags.PARROT_FOOD); }
    public static boolean parrotPoison(ItemStack stack) { return stack.is(ItemTags.PARROT_POISONOUS_FOOD); }
    public static void setThrower(net.minecraft.world.entity.item.ItemEntity item, net.minecraft.world.entity.Entity owner) { item.setThrower(owner); }
    public static boolean playerFood(ItemStack stack) { return stack.has(net.minecraft.core.component.DataComponents.FOOD); }
    public static boolean canPlayerEat(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        return food != null && player.canEat(food.canAlwaysEat());
    }
}
