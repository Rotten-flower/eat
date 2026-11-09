package dev.flower.eat.game;
import dev.flower.eat.mixin.ParrotFoodAccessor;
import net.minecraft.world.item.ItemStack;
public final class VersionFood {
    private VersionFood() {}
    public static boolean parrotFood(ItemStack stack) { return ParrotFoodAccessor.eat$tameFood().contains(stack.getItem()); }
    public static boolean parrotPoison(ItemStack stack) { return stack.is(ParrotFoodAccessor.eat$poisonFood()); }
    public static void setThrower(net.minecraft.world.entity.item.ItemEntity item, net.minecraft.world.entity.Entity owner) { item.setThrower(owner.getUUID()); }
    public static boolean playerFood(ItemStack stack) { return stack.isEdible(); }
    public static boolean canPlayerEat(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        var food = stack.getItem().getFoodProperties();
        return food != null && player.canEat(food.canAlwaysEat());
    }
}
