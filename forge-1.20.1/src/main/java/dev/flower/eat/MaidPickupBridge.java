package dev.flower.eat;

import dev.flower.eat.game.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import java.lang.reflect.Method;

/** 仅对原生治疗餐使用女仆的可取消拾取事件。 Use the maid’s cancellable pickup event only for native healing meals. */
final class MaidPickupBridge {
    @SuppressWarnings("unchecked") static void install() {
        String name = "com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent$ItemResultPre";
        try {
            Class<? extends Event> type = (Class<? extends Event>)Class.forName(name);
            Method maid = type.getMethod("getMaid"), item = type.getMethod("getEntityItem"), simulate = type.getMethod("isSimulate");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, type, event -> {
                try {
                    Mob mob = (Mob)maid.invoke(event);
                    ItemEntity food = (ItemEntity)item.invoke(event);
                    boolean reserved = ((EatingItem)food).eat$getStart() >= 0;
                    if (!reserved && (mob.getTarget() != null || !GroundFeeding.accepts(mob, food))) return;
                    if (!reserved && !(boolean)simulate.invoke(event)
                            && mob.distanceToSqr(food) <= GroundFeeding.reachSquared(mob))
                        ((EatingMob)mob).eat$getSession().begin(food);
                    event.setCanceled(true);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException("Touhou Little Maid pickup event API failed", failure);
                }
            });
        } catch (ClassNotFoundException absent) {
            // 未安装可选模组。 The optional mod is absent.
        } catch (NoSuchMethodException incompatible) {
            System.getLogger("eat.compat").log(System.Logger.Level.WARNING, "Maid pickup API is incompatible: " + incompatible.getMessage());
        }
    }
}
