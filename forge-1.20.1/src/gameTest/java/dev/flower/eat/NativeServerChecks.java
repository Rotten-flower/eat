package dev.flower.eat;
import dev.flower.eat.verification.NativeFeedingChecks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "eat")
public final class NativeServerChecks {
    @SubscribeEvent public static void start(ServerStartedEvent event) { NativeFeedingChecks.start(event.getServer()); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) { if (event.phase == TickEvent.Phase.END) NativeFeedingChecks.tick(); }
}
