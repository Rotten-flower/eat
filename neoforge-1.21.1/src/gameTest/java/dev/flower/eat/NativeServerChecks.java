package dev.flower.eat;
import dev.flower.eat.verification.NativeFeedingChecks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid = "eat")
public final class NativeServerChecks {
    @SubscribeEvent public static void start(ServerStartedEvent event) { NativeFeedingChecks.start(event.getServer()); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) { NativeFeedingChecks.tick(); }
}
