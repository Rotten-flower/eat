package dev.flower.eat.verification;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
public final class FabricServerChecks implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(NativeFeedingChecks::start);
        ServerTickEvents.END_SERVER_TICK.register(server -> NativeFeedingChecks.tick());
    }
}
