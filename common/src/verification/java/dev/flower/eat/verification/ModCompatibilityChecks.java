package dev.flower.eat.verification;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.flower.eat.game.*;
import dev.flower.eat.mixin.MobFeedInvoker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Loaded third-party species only: compares their real manual and dropped feeding branches. */
public final class ModCompatibilityChecks {
    private static final Set<String> MODS = Set.of("alexsmobs", "naturalist", "saintsdragons", "touhou_little_maid", "doggytalents", "friendsandfoes");
    private static final GameProfile OWNER = new GameProfile(UUID.nameUUIDFromBytes("eat-compat-owner".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "CompatOwner");
    private static final List<Map<String,Object>> results = new ArrayList<>();
    private record NavigationCase(Animal mob, ItemEntity food, float health, boolean[] chewed) {}
    private static final List<NavigationCase> navigation = new ArrayList<>();
    private static final List<net.minecraft.server.level.ServerPlayer> onlineOwners = new ArrayList<>();

    public static boolean run(ServerLevel level) throws Exception {
        if (!Boolean.getBoolean("eat.compat")) return true;
        boolean passed = true;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!MODS.contains(id.getNamespace())) continue;
            if (!net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(type)) {
                results.add(Map.of("entity", id.toString(), "status", "no registered living attributes; not instantiated"));
                continue;
            }
            var entity = type.create(level);
            if (!(entity instanceof Animal animal)) { if (entity != null) entity.discard(); continue; }
            prepare(animal, level);
            List<ItemStack> foods = new ArrayList<>();
            if (id.getNamespace().equals("touhou_little_maid")) {
                if (id.getPath().equals("maid")) foods.add(new ItemStack(Items.BREAD));
            } else {
                for (var item : BuiltInRegistries.ITEM) {
                    ItemStack food = new ItemStack(item);
                    if (animal.isFood(food) || (FeedingAdapters.hasAdapter(animal) && FeedingAdapters.accepts(animal, food, null))) foods.add(food);
                    if (foods.size() == 2) break;
                }
            }
            animal.discard();
            if (foods.isEmpty()) {
                results.add(Map.of("entity", id.toString(), "status", "no declared animal food; native pickup/feeding remains available"));
                continue;
            }
            for (ItemStack food : foods) {
                var result = new LinkedHashMap<String,Object>();
                result.put("entity", id.toString()); result.put("food", BuiltInRegistries.ITEM.getKey(food.getItem()).toString());
                Animal direct = (Animal)type.create(level), ground = (Animal)type.create(level);
                direct.setPos(.5, -60, 8.5); ground.setPos(4.5, -60, 8.5);
                for (Animal mob : List.of(direct, ground)) {
                    prepare(mob, level);
                    level.addFreshEntity(mob); mob.getRandom().setSeed(9123L);
                }
                ItemEntity dropped = new ItemEntity(level, ground.getX(), ground.getY(), ground.getZ(), food.copy());
                dropped.getItem().setCount(3); dropped.setNoPickUpDelay(); level.addFreshEntity(dropped);
                try {
                    FeedingPlayer player = new FeedingPlayer(level); player.prepare(OWNER, direct.getX(), direct.getY(), direct.getZ());
                    player.setItemInHand(InteractionHand.MAIN_HAND, food.copy());
                    if (id.toString().equals("alexsmobs:capuchin_monkey")) {
                        ItemEntity nativeFood = new ItemEntity(level, direct.getX(), direct.getY(), direct.getZ(), food.copy());
                        direct.getClass().getMethod("onGetItem", ItemEntity.class).invoke(direct, nativeFood);
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else if (id.getNamespace().equals("touhou_little_maid")) {
                        // Compare with its public meal contract, not the GUI-opening mobInteract branch.
                        Class<?> mealType = Class.forName("com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType");
                        @SuppressWarnings({"rawtypes","unchecked"}) Object heal = Enum.valueOf((Class)mealType, "HEAL_MEAL");
                        var manager = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager");
                        var contract = Class.forName("com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal");
                        var can = contract.getMethod("canMaidEat", direct.getClass(), ItemStack.class, InteractionHand.class);
                        var apply = contract.getMethod("onMaidEat", direct.getClass(), ItemStack.class, InteractionHand.class);
                        for (Object meal : (List<?>)manager.getMethod("getMaidMeals", mealType).invoke(null, heal)) {
                            if (!(boolean)can.invoke(meal, direct, food, InteractionHand.OFF_HAND)) continue;
                            direct.setItemInHand(InteractionHand.OFF_HAND, food.copy());
                            apply.invoke(meal, direct, direct.getOffhandItem(), InteractionHand.OFF_HAND); break;
                        }
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else if (direct instanceof AbstractHorse horse) horse.fedFood(player, player.getMainHandItem());
                    else ((MobFeedInvoker)direct).eat$feed(player, InteractionHand.MAIN_HAND);
                    if (!player.getMainHandItem().isEmpty()) {
                        result.put("status", "native manual branch did not consume in this state");
                    } else {
                        boolean consumed = GroundFeeding.eat(ground, dropped);
                        if (!consumed || dropped.getItem().getCount() != 2 || direct.getHealth() != ground.getHealth()
                                || direct.getAge() != ground.getAge() || direct.getInLoveTime() != ground.getInLoveTime()
                                || !ground.getPassengers().isEmpty()) throw new AssertionError("Native/dropped mismatch: consumed=" + consumed + ", health=" + direct.getHealth() + "/" + ground.getHealth() + ", love=" + direct.getInLoveTime()+"/"+ground.getInLoveTime());
                        if (ground instanceof TamableAnimal pet && !Objects.equals(pet.getOwnerUUID(), OWNER.getId())) throw new AssertionError("Owner changed");
                        if (id.getNamespace().equals("saintsdragons") && Class.forName("com.leon.saintsdragons.server.entity.base.DragonEntity").isInstance(direct)) {
                            var hunger = direct.getClass().getMethod("getHunger");
                            if (!hunger.invoke(direct).equals(hunger.invoke(ground))) throw new AssertionError("Native hunger mismatch");
                        }
                        if (id.getNamespace().equals("doggytalents")) {
                            var hunger = direct.getClass().getMethod("getDogHunger");
                            if (!hunger.invoke(direct).equals(hunger.invoke(ground))) throw new AssertionError("Native dog hunger mismatch");
                        }
                        result.put("status", "passed"); result.put("healthAfter", ground.getHealth()); result.put("loveTicks", ground.getInLoveTime());
                    }
                } catch (Throwable failure) {
                    passed = false; result.put("status", "failed"); result.put("error", failure.toString());
                    failure.printStackTrace();
                } finally { direct.discard(); ground.discard(); dropped.discard(); }
                results.add(result); System.out.println("EAT MOD COMPAT: " + result);
            }
        }
        Files.writeString(Path.of("eat-mod-compatibility.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed", passed, "results", results)));
        return passed;
    }

    private static void prepare(Animal mob, ServerLevel level) throws Exception {
        mob.setAge(0); mob.setHealth(Math.max(1F, mob.getMaxHealth()-6F));
        if (mob instanceof TamableAnimal pet) {
            FeedingPlayer owner = new FeedingPlayer(level); owner.prepare(OWNER, mob.getX(), mob.getY(), mob.getZ());
            pet.tame(owner); pet.setOwnerUUID(OWNER.getId());
        }
        if (mob.getClass().getName().equals("doggytalents.common.entity.Dog")) {
            float max = (float)mob.getClass().getMethod("getMaxHunger").invoke(mob);
            mob.getClass().getMethod("setDogHunger", float.class).invoke(mob, max-20F);
        }
    }

    public static boolean beginNavigation(ServerLevel level) throws Exception {
        if (!Boolean.getBoolean("eat.compat")) return false;
        Set<String> ids = Set.of("alexsmobs:capuchin_monkey", "naturalist:boar", "saintsdragons:stegonaut", "doggytalents:dog", "touhou_little_maid:maid", "friendsandfoes:moobloom");
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (!ids.contains(id)) continue;
            var matching = results.stream().filter(r -> id.equals(r.get("entity")) && "passed".equals(r.get("status"))).findFirst();
            if (matching.isEmpty()) continue;
            Animal mob = (Animal)type.create(level);
            double x = navigation.size()*14+.5;
            mob.setPos(x, -60, 8.5); prepare(mob, level);
            if (mob instanceof TamableAnimal pet) {
                // Tamed native sit goals intentionally stay put while their owner is offline.
                // Doggy Talents protects ownership changes; retain the owner assigned by tame().
                var identity = id.equals("doggytalents:dog") ? OWNER
                        : new GameProfile(UUID.nameUUIDFromBytes(("eat-owner-"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)), "CompatOwner");
                var onlineOwner = VersionCompatibilityChecks.onlineOwner(level, identity);
                onlineOwner.setPos(x-.5D, -60, 9.5D);
                level.addNewPlayer(onlineOwner); onlineOwners.add(onlineOwner);
                pet.setOwnerUUID(identity.getId());
            }
            if (mob instanceof TamableAnimal pet) { pet.setOrderedToSit(false); pet.setInSittingPose(false); }
            if (id.equals("alexsmobs:capuchin_monkey")) mob.getClass().getMethod("setCommand", int.class).invoke(mob, 0);
            if (id.equals("doggytalents:dog")) {
                mob.getClass().getMethod("setForceSit", boolean.class).invoke(mob, false);
                mob.setHealth(mob.getMaxHealth()-2F);
            }
            if (id.equals("saintsdragons:stegonaut")) mob.getClass().getMethod("setCommand", int.class).invoke(mob, 0);
            for (int dx = -2; dx <= 7; dx++) for (int dz = -3; dz <= 3; dz++)
                level.setBlock(new net.minecraft.core.BlockPos((int)x+dx,-61,8+dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            ItemStack stack = null;
            for (var item : BuiltInRegistries.ITEM) if (BuiltInRegistries.ITEM.getKey(item).toString().equals(matching.get().get("food"))) { stack = new ItemStack(item); break; }
            if (stack == null) throw new AssertionError("Missing registered navigation food");
            ItemEntity food = new ItemEntity(level, x+4, -60, 8.5, stack);
            food.setNoPickUpDelay(); food.setNoGravity(true); food.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            level.addFreshEntity(mob); level.addFreshEntity(food);
            navigation.add(new NavigationCase(mob, food, mob.getHealth(), new boolean[1]));
        }
        return !navigation.isEmpty();
    }
    public static void observeNavigation() {
        for (NavigationCase c : navigation) c.chewed()[0] |= ((EatingMob)c.mob()).eat$isChewing();
    }
    public static boolean finishNavigation() throws Exception {
        boolean passed = true;
        for (NavigationCase c : navigation) {
            var result = new LinkedHashMap<String,Object>();
            result.put("entity", BuiltInRegistries.ENTITY_TYPE.getKey(c.mob().getType()).toString());
            boolean ok = !c.food().isAlive() && c.chewed()[0];
            result.put("status", ok ? "passed" : "failed"); result.put("groundChewingObserved", c.chewed()[0]);
            result.put("foodConsumed", !c.food().isAlive()); result.put("finalX", c.mob().getX());
            result.put("healthBefore", c.health()); result.put("healthAfter", c.mob().getHealth());
            if (!ok) {
                result.put("mobTicks", c.mob().tickCount);
                result.put("foodAccepted", GroundFeeding.accepts(c.mob(), c.food()));
                result.put("target", String.valueOf(c.mob().getTarget()));
                result.put("sitting", c.mob() instanceof TamableAnimal pet && pet.isOrderedToSit());
                var path = c.mob().getNavigation().createPath(c.food(), 0);
                result.put("pathReachable", path != null && path.canReach());
                var adjacentPath = c.mob().getNavigation().createPath(c.food(), 1);
                result.put("adjacentPathReachable", adjacentPath != null && adjacentPath.canReach());
                if (adjacentPath != null && adjacentPath.getEndNode() != null) {
                    var end = adjacentPath.getEndNode();
                    result.put("endNode", List.of(end.x,end.y,end.z));
                }
                result.put("goalProbe", new GroundFoodGoal(c.mob()).canUse());
                result.put("goals", ((MobFeedInvoker)c.mob()).eat$getGoalSelector().getAvailableGoals().stream()
                        .map(g -> g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList());
                for (String method : List.of("getCommand", "isSitting", "canUpdateDogAi", "getMode", "isBusy", "forceSit")) {
                    try { result.put(method, String.valueOf(c.mob().getClass().getMethod(method).invoke(c.mob()))); }
                    catch (NoSuchMethodException absent) { /* Different mod. */ }
                }
            }
            results.add(result); passed &= ok; System.out.println("EAT MOD NAVIGATION: " + result);
            c.mob().discard(); c.food().discard();
        }
        Files.writeString(Path.of("eat-mod-compatibility.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed", passed, "results", results)));
        for (var owner : onlineOwners) {
            owner.discard();
        }
        return passed;
    }
}
