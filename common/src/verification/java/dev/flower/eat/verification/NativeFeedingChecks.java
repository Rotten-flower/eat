package dev.flower.eat.verification;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.flower.eat.game.*;
import dev.flower.eat.mixin.MobFeedInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Development-only native feeding comparisons and actual navigation checks. */
public final class NativeFeedingChecks {
    private static MinecraftServer server;
    private static ServerLevel level;
    private static int tick;
    private static boolean active;
    private static final List<String> checks = new ArrayList<>();
    private static final List<Entity> entities = new ArrayList<>();
    private static final GameProfile FEEDER = new GameProfile(UUID.nameUUIDFromBytes("eat-native-checks".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "Feeder");
    private static Cow walker;
    private static ItemEntity destination;
    private static Cow blocked;
    private static ItemEntity blockedFood;
    private static Cow competitor;
    private static ItemEntity competitorFood;
    private static net.minecraft.world.entity.animal.goat.Goat brainWalker;
    private static ItemEntity brainFood;
    private static FeedingPlayer brainFeeder;
    private static boolean modNavigation;

    public static void start(MinecraftServer minecraftServer) {
        if (!Boolean.getBoolean("eat.verify")) return;
        server = minecraftServer; level = server.overworld(); active = true;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks.add(message); System.out.println("EAT PASS: " + message);
    }

    private static <T extends Mob> T mob(EntityType<T> type, int x) {
        T mob = type.create(level);
        if (mob == null) throw new AssertionError("Could not create " + type);
        mob.setPos(x + .5, -60, .5);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob); entities.add(mob); return mob;
    }

    private static ItemEntity item(Mob mob, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), stack);
        item.setDeltaMovement(Vec3.ZERO); item.setNoPickUpDelay();
        level.addFreshEntity(item); entities.add(item); return item;
    }

    private static FeedingPlayer player(Mob mob) {
        FeedingPlayer player = new FeedingPlayer(level);
        player.prepare(FEEDER, mob.getX(), mob.getY(), mob.getZ());
        return player;
    }

    private static void assertions() throws Exception {
        var previous = new ArrayList<Entity>(); level.getAllEntities().forEach(previous::add);
        for (Entity entity : previous) if (!(entity instanceof net.minecraft.world.entity.player.Player)) entity.discard();
        command("gamerule doMobSpawning false"); command("gamerule doDaylightCycle false");
        command("time set noon"); command("difficulty normal");
        for (int x=-8; x<90; x++) for (int z=-4; z<5; z++) {
            level.setBlock(new BlockPos(x,-61,z), Blocks.STONE.defaultBlockState(), 2);
            for (int y=-60; y<=-54; y++) level.setBlock(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState(), 2);
        }
        configAssertions();
        playerFeedingAssertions();

        Cow fed = mob(EntityType.COW, 0), ground = mob(EntityType.COW, 4);
        FeedingPlayer player = player(fed); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        fed.mobInteract(player, InteractionHand.MAIN_HAND);
        ItemEntity food = item(ground, new ItemStack(Items.WHEAT, 4));
        check(GroundFeeding.eat(ground, food) && ground.isInLove() && ground.getInLoveTime() == fed.getInLoveTime(), "adult feeding uses the native love duration");
        check(food.getItem().getCount() == 3, "a stack loses exactly one serving");
        check(!GroundFeeding.eat(ground, food) && food.getItem().getCount() == 3, "animals already in love do not consume another serving");
        ground.resetLove(); ground.setAge(6000);
        check(!GroundFeeding.eat(ground, food) && ground.getAge() == 6000, "breeding cooldown is retained");

        Rabbit babyFed = mob(EntityType.RABBIT, 8), babyGround = mob(EntityType.RABBIT, 12);
        babyFed.setAge(-24000); babyGround.setAge(-24000);
        player = player(babyFed); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DANDELION));
        babyFed.mobInteract(player, InteractionHand.MAIN_HAND);
        check(GroundFeeding.eat(babyGround, item(babyGround, new ItemStack(Items.DANDELION))) && babyGround.getAge() == babyFed.getAge(), "baby growth matches native feeding");

        Cat catFed = mob(EntityType.CAT, 16), catGround = mob(EntityType.CAT, 20);
        catFed.tame(player(catFed)); catGround.tame(player(catGround));
        catFed.setOwnerUUID(FEEDER.getId()); catGround.setOwnerUUID(FEEDER.getId());
        catFed.setHealth(2F); catGround.setHealth(2F); catGround.setOrderedToSit(true);
        player = player(catFed); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON));
        catFed.mobInteract(player, InteractionHand.MAIN_HAND);
        check(GroundFeeding.eat(catGround, item(catGround, new ItemStack(Items.SALMON))) && catGround.getHealth() == catFed.getHealth(), "pet healing matches native feeding");
        check(catGround.isOrderedToSit(), "ground food does not issue a sit or stand command");
        check(!GroundFeeding.eat(catGround, item(catGround, new ItemStack(Items.WHEAT))), "unrelated food is rejected without changing the pet");

        Horse horseFed = mob(EntityType.HORSE, 24), horseGround = mob(EntityType.HORSE, 28);
        horseFed.setHealth(2F); horseGround.setHealth(2F);
        player = player(horseFed);
        horseFed.fedFood(player, new ItemStack(Items.APPLE));
        check(GroundFeeding.eat(horseGround, item(horseGround, new ItemStack(Items.APPLE))) && horseGround.getHealth() == horseFed.getHealth()
                && horseGround.getTemper() == horseFed.getTemper(), "horse healing and temper use the native food branch");
        check(horseGround.getPassengers().isEmpty(), "the feeding context never mounts a horse");

        Axolotl axolotl = mob(EntityType.AXOLOTL, 32);
        ItemEntity bucket = item(axolotl, new ItemStack(Items.TROPICAL_FISH_BUCKET));
        check(GroundFeeding.eat(axolotl, bucket) && axolotl.isInLove() && !bucket.isAlive(), "bucket food consumes one physical bucket");
        check(level.getEntitiesOfClass(ItemEntity.class, axolotl.getBoundingBox().inflate(2), e -> e.getItem().is(Items.WATER_BUCKET)).size() == 1,
                "native feeding leaves exactly one empty water bucket on the ground");

        Chicken chicken = mob(EntityType.CHICKEN, 36);
        food = item(chicken, new ItemStack(Items.WHEAT_SEEDS)); food.setDefaultPickUpDelay();
        check(!GroundFeeding.eat(chicken, food), "freshly dropped items respect their pickup delay");

        Cat wildFed = mob(EntityType.CAT, 42), wildGround = mob(EntityType.CAT, 46);
        FeedingPlayer source = player(wildFed);
        level.addFreshEntity(source); entities.add(source);
        source.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
        wildFed.getRandom().setSeed(1L); wildGround.getRandom().setSeed(1L);
        wildFed.mobInteract(source, InteractionHand.MAIN_HAND);
        food = item(wildGround, new ItemStack(Items.COD, 2)); VersionFood.setThrower(food, source);
        check(GroundFeeding.eat(wildGround, food) && wildGround.isTame() == wildFed.isTame()
                && Objects.equals(wildGround.getOwnerUUID(), wildFed.getOwnerUUID()), "dropped taming food uses native chance and the thrower's identity");
        check(food.getItem().getCount() == 1, "taming attempts consume one dropped serving");

        Cow winner = mob(EntityType.COW, 40), loser = mob(EntityType.COW, 40);
        food = item(winner, new ItemStack(Items.WHEAT));
        check(GroundFeeding.eat(winner, food) && !GroundFeeding.eat(loser, food), "two animals cannot consume the same serving");

        Cow chewing = mob(EntityType.COW, 76);
        ItemEntity stack = item(chewing, new ItemStack(Items.WHEAT, 3));
        EatingSession meal = ((EatingMob)chewing).eat$getSession();
        long time = level.getGameTime();
        check(meal.begin(stack) && meal.active() && !chewing.isInLove(), "a serving starts chewing without applying the feeding effect");
        check(chewing.getXRot() > 45F, "chewing lowers the head toward the ground");
        var morsels = level.getEntitiesOfClass(ItemEntity.class, chewing.getBoundingBox().inflate(3), e -> e.isAlive() && e.getItem().is(Items.WHEAT));
        check(morsels.stream().mapToInt(e -> e.getItem().getCount()).sum() == 3, "reserving a serving preserves the total physical food count: " + morsels.stream().map(e -> e.getId()+":"+e.getItem().getCount()).toList());
        ItemEntity held = morsels.stream().filter(e -> ((EatingItem)e).eat$getStart() >= 0).findFirst().orElseThrow();
        var saved = held.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        check(saved.getShort("PickupDelay") == 0 && !saved.getBoolean("NoGravity")
                && saved.getList("Pos",6).getDouble(0) == stack.getX(), "a saved unfinished serving reloads as normal reachable ground food");
        try {
            for (int elapsed=0;elapsed<EatingSession.DURATION;elapsed++) {
                ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+elapsed);meal.tick();
                check(meal.active() && !chewing.isInLove(), "feeding waits through chewing tick " + elapsed);
            }
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+EatingSession.DURATION);meal.tick();
            check(!meal.active() && chewing.isInLove() && stack.getItem().getCount()==2 && !held.isAlive(), "a completed 32-tick meal consumes one serving and applies native feeding");
            chewing.resetLove();
            check(meal.begin(stack), "the next serving starts a separate meal");
            chewing.hurtTime=10;meal.tick();chewing.hurtTime=0;
            check(!meal.active() && !chewing.isInLove()
                    && level.getEntitiesOfClass(ItemEntity.class, chewing.getBoundingBox().inflate(3), e -> e.isAlive() && e.getItem().is(Items.WHEAT)).stream().mapToInt(e -> e.getItem().getCount()).sum()==2,
                    "damage interrupts chewing without consuming food or applying the effect");
        } finally { ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time); }

        for (Entity entity : entities) entity.discard(); entities.clear();
        walker = mob(EntityType.COW, 48); destination = item(walker, new ItemStack(Items.WHEAT, 2));
        destination.setPos(walker.getX()+4, -60, walker.getZ()); destination.setNoGravity(true);
        blocked = mob(EntityType.COW, 64); blockedFood = item(blocked, new ItemStack(Items.WHEAT, 2));
        blockedFood.setPos(blocked.getX()+2, -60, blocked.getZ()); blockedFood.setNoGravity(true);
        for (int x=63; x<=65; x++) for (int y=-60; y<=-57; y++) for(int z=-1; z<=1; z++)
            if (x==63 || x==65 || z==-1 || z==1) level.setBlock(new BlockPos(x,y,z), Blocks.STONE.defaultBlockState(),2);
        // Disable roaming for a stationary competition-free comparison, retaining the food goal.
        for (Cow cow : List.of(walker, blocked)) {
            var goals = ((MobFeedInvoker)cow).eat$getGoalSelector();
            for (var wrapped : List.copyOf(goals.getAvailableGoals())) if (!(wrapped.getGoal() instanceof GroundFoodGoal)) goals.removeGoal(wrapped.getGoal());
        }
        competitor = mob(EntityType.COW, 84);
        var competingGoals = ((MobFeedInvoker)competitor).eat$getGoalSelector();
        competingGoals.addGoal(3, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return true; }
            @Override public boolean canContinueToUse() { return true; }
            @Override public void tick() { competitor.getNavigation().moveTo(88.5, -60, .5, 1.25); }
        });
        competitorFood = item(competitor, new ItemStack(Items.WHEAT, 2));
        competitorFood.setPos(82.5,-60,.5);
        brainWalker = mob(EntityType.GOAT, 8);
        brainFeeder = player(brainWalker);
        brainFeeder.setPos(1.5, -60, .5);
        brainFeeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        brainFood = item(brainWalker, new ItemStack(Items.WHEAT, 2));
        brainFood.setPos(12.5, -60, .5);
    }

    private static void configAssertions() throws Exception {
        Path folder = Files.createTempDirectory(Path.of("."), "eat-config-check-");
        Path config = folder.resolve("eat.json");
        Cow cow = mob(EntityType.COW, 0);
        Chicken chicken = mob(EntityType.CHICKEN, 3);
        ItemEntity wheat = item(cow, new ItemStack(Items.WHEAT, 3));
        try {
            EatConfig.load(config);
            check(Files.isRegularFile(config) && EatConfig.allowsGround(cow) && EatConfig.allowsPlayers(), "missing config creates enabled defaults");
            Files.writeString(config, "{broken");
            EatConfig.load(config);
            check(Files.readString(config).equals("{broken") && EatConfig.allowsPlayers(), "broken JSON is retained and safe defaults load");
            Files.writeString(config, "{\"groundFeeding\":\"false\",\"playerFeeding\":false,\"excludedEntities\":[\"minecraft:cow\",4,\"bad id\"]}");
            EatConfig.load(config);
            check(!EatConfig.allowsGround(cow) && EatConfig.allowsGround(chicken) && !EatConfig.allowsPlayers(), "invalid fields retain valid exclusions and player switch");
            check(!GroundFeeding.accepts(cow, wheat) && !new GroundFoodGoal(cow).canUse(), "excluded species cannot reserve or search for ground food");
            FeedingPlayer feeder = player(cow), target = player(chicken);
            target.getFoodData().setFoodLevel(10);
            feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));
            check(PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS
                    && feeder.getMainHandItem().getCount() == 2 && target.getMainHandItem().isEmpty() && target.getOffhandItem().isEmpty(), "disabled player feeding transfers no items");
            Files.writeString(config, "{}"); EatConfig.load(config);
            EatingSession meal = ((EatingMob)cow).eat$getSession();
            check(meal.begin(wheat), "enabled defaults permit a real chewing session; accepts=" + GroundFeeding.accepts(cow, wheat)
                    + ", visible=" + cow.hasLineOfSight(wheat) + ", distance=" + cow.distanceToSqr(wheat) + ", hurt=" + cow.hurtTime
                    + ", foodAlive=" + wheat.isAlive() + ", age=" + cow.getAge() + ", target=" + cow.getTarget());
            Files.writeString(config, "{\"groundFeeding\":false}"); EatConfig.load(config); meal.tick();
            check(!meal.active() && !cow.isInLove() && !GroundFeeding.accepts(cow, wheat) && !new GroundFoodGoal(cow).canUse()
                    && level.getEntitiesOfClass(ItemEntity.class,cow.getBoundingBox().inflate(2),e -> e.isAlive() && e.getItem().is(Items.WHEAT)).stream().mapToInt(e -> e.getItem().getCount()).sum() == 3,
                    "disabled ground feeding releases unfinished servings without consuming food");
        } finally {
            Files.writeString(config, "{}"); EatConfig.load(config);
            Files.deleteIfExists(config); Files.deleteIfExists(folder);
            cow.discard(); chicken.discard(); wheat.discard();
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class,cow.getBoundingBox().inflate(2))) e.discard();
        }
    }

    private static void playerFeedingAssertions() {
        FeedingPlayer feeder = new FeedingPlayer(level), target = new FeedingPlayer(level);
        feeder.prepare(FEEDER, 0.5, -60, 0.5);
        target.prepare(new GameProfile(UUID.randomUUID(), "Recipient"), 1.5, -60, 0.5);
        target.getFoodData().setFoodLevel(10);
        target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));
        check(PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction()
                && target.isUsingItem() && target.getFoodData().getFoodLevel() == 10, "player feeding starts native eating without an early hunger effect");
        check(feeder.getMainHandItem().getCount() == 2 && target.getOffhandItem().getCount() == 1
                && target.getMainHandItem().is(Items.IRON_SWORD), "player feeding transfers one serving and preserves occupied hands");
        check(!PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction(), "a second feeder cannot overwrite an ongoing player meal");
        for (int i = 0; i < 32; i++) { target.tick(); feeder.tick(); feeder.tickCount++; }
        check(target.getFoodData().getFoodLevel() == 15 && target.getOffhandItem().isEmpty(), "player hunger restoration and eating duration follow the native food: hunger=" + target.getFoodData().getFoodLevel() + ", hand=" + target.getOffhandItem() + ", remaining=" + target.getUseItemRemainingTicks());
        target.getFoodData().setFoodLevel(20);
        check(!PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction() && feeder.getMainHandItem().getCount() == 2, "full players reject ordinary food without consuming the feeder's items");
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 2));
        check(PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction(), "always-edible food keeps its native full-hunger eligibility");
        for (int i = 0; i < 32; i++) { target.tick(); feeder.tick(); feeder.tickCount++; }
        check(target.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "player feeding retains the native food's status effects");
        target.getFoodData().setFoodLevel(10);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSHROOM_STEW));
        check(PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction(), "container food starts native player consumption");
        for (int i = 0; i < 32; i++) { target.tick(); feeder.tick(); feeder.tickCount++; }
        check(target.getOffhandItem().is(Items.BOWL), "player container food returns its native bowl without duplication");
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));
        check(!PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction() && feeder.getMainHandItem().getCount() == 2, "occupied player hands never lose equipment or food");
        target.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        target.setPos(10.5, -60, .5);
        check(!PlayerFeeding.interact(feeder, target, InteractionHand.MAIN_HAND).consumesAction(), "player feeding rejects requests outside three blocks");
        check(PlayerFeeding.interact(feeder, feeder, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS, "player feeding cannot target the feeder itself");
    }

    public static void tick() {
        if (!active) return;
        try {
            tick++;
            // Force-load before spawning fixtures: 1.21 activates entity sections asynchronously.
            if (tick == 1) {
                command("forceload add -16 -16 95 16");
                for (int x = -1; x <= 5; x++) for (int z = -1; z <= 1; z++) level.getChunk(x, z);
            }
            if (tick == 30) assertions();
            if (brainWalker != null) {
                brainWalker.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.TEMPTING_PLAYER, brainFeeder);
            }
            if (modNavigation) ModCompatibilityChecks.observeNavigation();
            if (tick == 230) {
                check(walker.isInLove() && destination.getItem().getCount() == 1, "animal paths to reachable ground food and eats one serving");
                check(!blocked.isInLove() && blockedFood.getItem().getCount() == 2, "food behind a solid enclosure is not consumed");
                check(competitor.isInLove() && competitorFood.getItem().getCount()==1, "a competing follow goal yields to reachable ground food");
                check(brainWalker.isInLove() && brainFood.getItem().getCount()==1, "native Brain temptation yields to reachable ground food");
                check(server.isDedicatedServer(), "dedicated server loads without client classes");
                check(ModCompatibilityChecks.run(level), "loaded third-party species match their native feeding contracts");
                modNavigation = ModCompatibilityChecks.beginNavigation(level);
                if (!modNavigation) { write(true, null); active=false; server.halt(false); }
            }
            if (tick == 430 && modNavigation) {
                check(ModCompatibilityChecks.finishNavigation(), "loaded mod pets navigate to ground food and chew before consumption");
                write(true, null); active=false; server.halt(false);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            try { write(false, failure.toString()); } catch (Exception io) { io.printStackTrace(); }
            active=false; server.halt(false);
        }
    }

    private static void command(String command) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command); }
    private static void write(boolean passed, String error) throws Exception {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("passed", passed); result.put("checks", checks); result.put("error", error);
        Files.writeString(Path.of("eat-native-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
}
