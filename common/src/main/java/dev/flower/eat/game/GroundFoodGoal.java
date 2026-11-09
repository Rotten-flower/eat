package dev.flower.eat.game;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import dev.flower.eat.mixin.MobFeedInvoker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import java.util.EnumSet;

/** 加入有搜索预算的觅食目标，沿用已有动物 AI。 Add one bounded food-seeking goal alongside existing animal AI. */
public final class GroundFoodGoal extends Goal {
    public static final int PRIORITY = 2;
    private final Mob mob;
    private ItemEntity food;
    private Path path;
    private long nextSearch;
    private long lastSearch;
    private long expires;
    private long nextPath;

    public GroundFoodGoal(Mob mob) {
        this.mob = mob;
        setFlags(mob instanceof net.minecraft.world.entity.animal.Parrot ? EnumSet.of(Flag.MOVE) : EnumSet.of(Flag.MOVE, Flag.LOOK));
        nextSearch = mob.getId() % 40;
    }

    @Override public boolean canUse() {
        if (!EatConfig.allowsGround(mob)) return false;
        long time = mob.level().getGameTime();
        if (((EatingMob)mob).eat$isChewing() || mob.getTarget() != null || mob.isPassenger() || mob.isVehicle()
                || (mob instanceof TamableAnimal pet && pet.isOrderedToSit())) return false;
        // 选择器启动前，食物吸引检查可能已找到地面食物。 A temptation probe may find food before the selector starts this goal.
        if (food != null && time - lastSearch <= 40 && mob.distanceToSqr(food) < 64D && GroundFeeding.accepts(mob, food)) return true;
        if (time < nextSearch && !(time - lastSearch >= 10 && isTempted())) return false;
        lastSearch = time;
        nextSearch = time + 40;
        food = null;
        double best = 64D;
        int pathBudget = 3;
        var candidates = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(8D, 3D, 8D),
                item -> GroundFeeding.accepts(mob, item));
        candidates.sort(java.util.Comparator.comparingDouble(mob::distanceToSqr));
        for (ItemEntity item : candidates) {
            double distance = mob.distanceToSqr(item);
            if (distance >= best) continue;
            if (distance <= GroundFeeding.reachSquared(mob) && mob.hasLineOfSight(item)) {
                food = item; path = null; break;
            }
            if (pathBudget-- <= 0) break;
            Path candidate = mob.getNavigation().createPath(item, FeedingAdapters.pathAccuracy(mob));
            if (candidate != null && candidate.canReach() && candidate.getEndNode() != null) {
                var end = candidate.getEndNode();
                if (item.distanceToSqr(end.x + .5D, end.y, end.z + .5D) > GroundFeeding.reachSquared(mob)) continue;
                food = item; path = candidate; best = distance; break;
            }
        }
        return food != null;
    }

    private boolean isTempted() {
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.IS_TEMPTED)
                && mob.getBrain().getMemory(MemoryModuleType.IS_TEMPTED).orElse(false)) return true;
        for (var wrapped : ((MobFeedInvoker)mob).eat$getGoalSelector().getAvailableGoals())
            if (wrapped.isRunning() && wrapped.getGoal() instanceof TemptGoal) return true;
        return false;
    }

    @Override public void start() {
        ((EatingMob)mob).eat$setSeekingGroundFood(true);
        expires = mob.level().getGameTime() + 120;
        nextPath = mob.level().getGameTime() + 20;
        if (path != null) mob.getNavigation().moveTo(path, 1D);
    }

    @Override public boolean canContinueToUse() {
        if (!EatConfig.allowsGround(mob)) return false;
        if (((EatingMob)mob).eat$isChewing()) return true;
        return food != null && GroundFeeding.accepts(mob, food) && mob.getTarget() == null
                && mob.level().getGameTime() < expires && mob.distanceToSqr(food) <= 100D
                && !(mob instanceof TamableAnimal pet && pet.isOrderedToSit());
    }

    @Override public void tick() {
        if (((EatingMob)mob).eat$isChewing()) { mob.getNavigation().stop(); return; }
        if (food == null) return;
        if (!(mob instanceof net.minecraft.world.entity.animal.Parrot)) mob.getLookControl().setLookAt(food, 30F, 30F);
        if (mob.distanceToSqr(food) <= GroundFeeding.reachSquared(mob) && mob.hasLineOfSight(food)) {
            ((EatingMob)mob).eat$getSession().begin(food);
            food = null;
            nextSearch = mob.level().getGameTime() + 20;
            mob.getNavigation().stop();
        } else if (mob.level().getGameTime() >= nextPath) {
            nextPath = mob.level().getGameTime() + 20;
            mob.getNavigation().moveTo(food, 1D);
        }
    }

    @Override public void stop() {
        ((EatingMob)mob).eat$setSeekingGroundFood(false);
        if (((EatingMob)mob).eat$isChewing()) ((EatingMob)mob).eat$getSession().cancel();
        food = null; path = null; mob.getNavigation().stop();
    }
}
