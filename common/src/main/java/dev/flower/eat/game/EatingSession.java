package dev.flower.eat.game;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 保留一份实体食物，咀嚼 32 tick 后调用原生喂食。 Reserve one physical serving, chew for 32 ticks, then apply native feeding. */
public final class EatingSession {
    public static final int DURATION = 32;
    private final Mob mob;
    private ItemEntity serving;
    private ItemStack food = ItemStack.EMPTY;
    private long started;
    private Vec3 groundPosition;
    private float previousPitch;

    public EatingSession(Mob mob) { this.mob = mob; }
    public boolean active() { return serving != null; }

    public boolean begin(ItemEntity source) {
        if (active() || !(mob.level() instanceof ServerLevel level) || mob.hurtTime > 0 || mob.getTarget() != null
                || !GroundFeeding.accepts(mob, source) || !mob.hasLineOfSight(source)
                || mob.distanceToSqr(source) > GroundFeeding.reachSquared(mob)) return false;
        Vec3 returnPosition = source.position();
        groundPosition = returnPosition;
        previousPitch = mob.getXRot();
        food = source.getItem().copy(); food.setCount(1);
        if (source.getItem().getCount() == 1) {
            serving = source;
        } else {
            // 这一份仍保留为实体，中断时退回而不消耗。 Keep this serving physical so interruption returns it without consumption.
            serving = new ItemEntity(level, source.getX(), source.getY(), source.getZ(), food.copy());
            if (source.getOwner() != null) VersionFood.setThrower(serving, source.getOwner());
            ItemStack remainder = source.getItem().copy(); remainder.shrink(1); source.setItem(remainder);
        }
        started = level.getGameTime();
        ((EatingItem)serving).eat$reserve(mob, returnPosition);
        serving.setNoGravity(true); serving.setNeverPickUp(); serving.setDeltaMovement(Vec3.ZERO);
        serving.setPos(groundPosition); serving.hasImpulse = true;
        if (serving != source) level.addFreshEntity(serving);
        mob.getNavigation().stop();
        lookDown();
        return true;
    }

    public void tick() {
        if (!active() || !(mob.level() instanceof ServerLevel level)) return;
        if (!EatConfig.allowsGround(mob) || !mob.isAlive() || mob.isNoAi() || mob.hurtTime > 0 || mob.getTarget() != null
                || mob.isPassenger() || !serving.isAlive() || ((EatingItem)serving).eat$getEaterId() != mob.getId()) {
            cancel(); return;
        }
        mob.getNavigation().stop();
        if (mob.distanceToSqr(groundPosition) > GroundFeeding.reachSquared(mob) || !mob.hasLineOfSight(serving)) { cancel(); return; }
        serving.setPos(groundPosition); serving.setDeltaMovement(Vec3.ZERO);
        lookDown();
        long elapsed = level.getGameTime() - started;
        if (elapsed >= DURATION) {
            ItemEntity finished = serving;
            ((EatingItem)finished).eat$release(false);
            finished.setPos(mob.position());
            boolean consumed = GroundFeeding.eat(mob, finished);
            if (!consumed && finished.isAlive()) ((EatingItem)finished).eat$release(true);
            if (consumed) mob.playSound(SoundEvents.PLAYER_BURP, .35F, 1F);
            clear(); return;
        }
        if (elapsed % 4 == 0) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food), groundPosition.x, groundPosition.y + .2D, groundPosition.z,
                    4, .08D, .06D, .08D, .035D);
            mob.playSound(SoundEvents.GENERIC_EAT, .7F, .9F + mob.getRandom().nextFloat() * .2F);
        }
    }

    private void lookDown() {
        mob.getLookControl().setLookAt(groundPosition.x, groundPosition.y + .15D, groundPosition.z, 90F, 90F);
        // 保持低头进食姿态，避免空闲视线目标覆盖。 Keep the lowered pose when an idle look goal is running.
        mob.setXRot(58F);
    }

    public void cancel() {
        if (serving != null && serving.isAlive()) ((EatingItem)serving).eat$release(true);
        clear();
    }

    private void clear() { mob.setXRot(previousPitch); serving = null; food = ItemStack.EMPTY; groundPosition = null; }
}

