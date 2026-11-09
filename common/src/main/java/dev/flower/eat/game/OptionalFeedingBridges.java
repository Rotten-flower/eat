package dev.flower.eat.game;

import dev.flower.eat.mixin.MobFeedInvoker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/** 调用可选模组的公开喂食接口，不内嵌或强制依赖。 Call optional mods’ public feeding contracts without bundling or requiring them. */
public final class OptionalFeedingBridges {
    private static final System.Logger LOG = System.getLogger("eat.compat");
    private OptionalFeedingBridges() {}

    public static void install() {
        installMaid(); installDragons(); installDoggyTalents(); installCapuchin();
    }

    @SuppressWarnings("unchecked") private static void installMaid() {
        try {
            Class<?> maid = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid", false, OptionalFeedingBridges.class.getClassLoader());
            Class<?> mealType = Class.forName("com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType");
            Object healing = Enum.valueOf((Class)mealType, "HEAL_MEAL");
            Method meals = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager").getMethod("getMaidMeals", mealType);
            Class<?> meal = Class.forName("com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal");
            Method canEat = meal.getMethod("canMaidEat", maid, ItemStack.class, InteractionHand.class);
            Method eat = meal.getMethod("onMaidEat", maid, ItemStack.class, InteractionHand.class);
            Method task = maid.getMethod("getTask");
            Method enabled = task.getReturnType().getMethod("enableEating", maid);
            FeedingAdapters.register((Class<? extends Mob>)maid, new FeedingAdapters.Adapter() {
                private InteractionHand emptyHand(Mob mob) {
                    return mob.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : mob.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND : null;
                }
                private Object findMeal(Mob mob, ItemStack stack, InteractionHand hand) {
                    for (Object value : (List<?>)call(meals, null, healing))
                        if ((boolean)call(canEat, value, mob, stack, hand)) return value;
                    return null;
                }
                @Override public boolean accepts(Mob mob, ItemStack stack, Player thrower) {
                    InteractionHand hand = emptyHand(mob);
                    return ((TamableAnimal)mob).isTame() && !mob.isUsingItem() && !mob.isSleeping() && hand != null
                            && mob.getMaxHealth() - mob.getHealth() >= 2F && (boolean)call(enabled, call(task, mob), mob)
                            && findMeal(mob, stack, hand) != null;
                }
                @Override public void feed(Mob mob, Player context, InteractionHand ignored) {
                    ItemStack serving = context.getMainHandItem();
                    InteractionHand hand = emptyHand(mob);
                    if (hand == null || !accepts(mob, serving, null)) return;
                    Object recipe = findMeal(mob, serving, hand);
                    // 原生餐食流程负责最终消耗和容器处理。 The native meal owns final consumption and container handling.
                    mob.setItemInHand(hand, serving.copy());
                    FeedingVisuals.syncHand(mob, hand);
                    call(eat, recipe, mob, mob.getItemInHand(hand), hand);
                    context.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            });
            LOG.log(System.Logger.Level.INFO, "Enabled Touhou Little Maid native meal bridge.");
        } catch (ClassNotFoundException absent) {
            if (!"com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid".equals(absent.getMessage()))
                LOG.log(System.Logger.Level.WARNING, "Touhou Little Maid meal API class is missing: " + absent.getMessage());
        } catch (NoSuchMethodException incompatible) {
            LOG.log(System.Logger.Level.WARNING, "Touhou Little Maid meal API is incompatible: " + incompatible.getMessage());
        }
    }

    @SuppressWarnings("unchecked") private static void installDragons() {
        try {
            Class<?> dragon = Class.forName("com.leon.saintsdragons.server.entity.base.DragonEntity", false, OptionalFeedingBridges.class.getClassLoader());
            var cooldowns = new ClassValue<Method>() {
                @Override protected Method computeValue(Class<?> type) {
                    try { return type.getMethod("canFeed"); } catch (NoSuchMethodException missing) { return null; }
                }
            };
            FeedingAdapters.register((Class<? extends Mob>)dragon, new FeedingAdapters.Adapter() {
                @Override public boolean accepts(Mob mob, ItemStack stack, Player thrower) {
                    if (!((Animal)mob).isFood(stack) || (!((TamableAnimal)mob).isTame() && thrower == null)) return false;
                    Method cooldown = cooldowns.get(mob.getClass());
                    return cooldown == null || (boolean)call(cooldown, mob);
                }
                @Override public void feed(Mob mob, Player context, InteractionHand hand) { ((MobFeedInvoker)mob).eat$feed(context, hand); }
            });
            LOG.log(System.Logger.Level.INFO, "Enabled Saints & Sinners Dragons native food bridge.");
        } catch (ClassNotFoundException absent) {
            if (!"com.leon.saintsdragons.server.entity.base.DragonEntity".equals(absent.getMessage()))
                LOG.log(System.Logger.Level.WARNING, "Saint's Dragons food API class is missing: " + absent.getMessage());
        }
    }

    private static Object call(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (IllegalAccessException | InvocationTargetException failure) {
            Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
            throw new IllegalStateException("Native feeding API failed: " + method.getDeclaringClass().getName() + "." + method.getName(), cause);
        }
    }

    @SuppressWarnings("unchecked") private static void installDoggyTalents() {
        try {
            Class<?> dog = Class.forName("doggytalents.common.entity.Dog", false, OptionalFeedingBridges.class.getClassLoader());
            Class<?> base = Class.forName("doggytalents.api.inferface.AbstractDog");
            Class<?> registry = Class.forName("doggytalents.api.feature.FoodHandler");
            Method match = registry.getMethod("getMatch", base, ItemStack.class, net.minecraft.world.entity.Entity.class);
            Method available = base.getMethod("canStillEat");
            Method consume = Class.forName("doggytalents.api.inferface.IDogFoodHandler").getMethod("consume", base, ItemStack.class, net.minecraft.world.entity.Entity.class);
            FeedingAdapters.register((Class<? extends Mob>)dog, new FeedingAdapters.Adapter() {
                @Override public int pathAccuracy() { return 1; }
                @Override public boolean accepts(Mob mob, ItemStack stack, Player thrower) {
                    return ((TamableAnimal)mob).isTame() && (boolean)call(available, mob)
                            && ((java.util.Optional<?>)call(match, null, mob, stack, thrower)).isPresent();
                }
                @Override public void feed(Mob mob, Player context, InteractionHand hand) {
                    var handler = (java.util.Optional<?>)call(match, null, mob, context.getItemInHand(hand), context);
                    handler.ifPresent(value -> call(consume, value, mob, context.getItemInHand(hand), context));
                }
            });
            LOG.log(System.Logger.Level.INFO, "Enabled Doggy Talents Next native food registry bridge.");
        } catch (ClassNotFoundException absent) {
            if (!"doggytalents.common.entity.Dog".equals(absent.getMessage()))
                LOG.log(System.Logger.Level.WARNING, "Doggy Talents food API class is missing: " + absent.getMessage());
        } catch (NoSuchMethodException incompatible) {
            LOG.log(System.Logger.Level.WARNING, "Doggy Talents food API is incompatible: " + incompatible.getMessage());
        }
    }

    @SuppressWarnings("unchecked") private static void installCapuchin() {
        try {
            Class<?> monkey = Class.forName("com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey", false, OptionalFeedingBridges.class.getClassLoader());
            Method food = monkey.getMethod("canTargetItem", ItemStack.class);
            Method pickup = monkey.getMethod("onGetItem", net.minecraft.world.entity.item.ItemEntity.class);
            FeedingAdapters.register((Class<? extends Mob>)monkey, new FeedingAdapters.Adapter() {
                @Override public boolean accepts(Mob mob, ItemStack stack, Player thrower) {
                    return (boolean)call(food, mob, stack) && (((TamableAnimal)mob).isTame() || thrower != null);
                }
                @Override public void feed(Mob mob, Player context, InteractionHand hand) {
                    var serving = new net.minecraft.world.entity.item.ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), context.getItemInHand(hand).copy());
                    VersionFood.setThrower(serving, context);
                    call(pickup, mob, serving);
                    context.getItemInHand(hand).shrink(1);
                }
            });
        } catch (ClassNotFoundException absent) {
            // 未安装可选依赖。 The optional dependency is absent.
        } catch (NoSuchMethodException incompatible) {
            LOG.log(System.Logger.Level.WARNING, "Alex's Mobs capuchin pickup API is incompatible: " + incompatible.getMessage());
        }
    }
}
