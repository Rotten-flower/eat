package dev.flower.eat.game;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;

/** 可选原生喂食桥接，在模组初始化时注册。 Optional native feeding bridges registered during mod initialization. */
public final class FeedingAdapters {
    public interface Adapter {
        boolean accepts(Mob mob, ItemStack stack, Player thrower);
        void feed(Mob mob, Player feedingContext, InteractionHand hand);
        default int pathAccuracy() { return 0; }
    }
    private static final Map<Class<?>, Adapter> ADAPTERS = new LinkedHashMap<>();
    private static volatile ClassValue<Adapter> lookup = newLookup();
    private static ClassValue<Adapter> newLookup() {
        return new ClassValue<>() {
            @Override protected Adapter computeValue(Class<?> type) {
                for (var entry : ADAPTERS.entrySet()) if (entry.getKey().isAssignableFrom(type)) return entry.getValue();
                return null;
            }
        };
    }
    private FeedingAdapters() {}
    public static void register(Class<? extends Mob> type, Adapter adapter) {
        if (ADAPTERS.putIfAbsent(type, adapter) != null) throw new IllegalStateException("Duplicate feeding adapter: " + type.getName());
        lookup = newLookup();
        // 世界加载前注册，避免每 tick 反射检查食物。 Register before worlds load; avoid per-tick reflective food probing.
    }
    public static boolean hasAdapter(Mob mob) { return lookup.get(mob.getClass()) != null; }
    public static boolean accepts(Mob mob, ItemStack stack, Player thrower) { return lookup.get(mob.getClass()).accepts(mob, stack, thrower); }
    public static void feed(Mob mob, Player context, InteractionHand hand) { lookup.get(mob.getClass()).feed(mob, context, hand); }
    public static int pathAccuracy(Mob mob) { var adapter = lookup.get(mob.getClass()); return adapter == null ? 0 : Math.max(0, Math.min(1, adapter.pathAccuracy())); }
}
