package dev.flower.eat;
import net.fabricmc.api.ModInitializer;
import dev.flower.eat.core.ModIdentity;
public final class ModEntrypoint implements ModInitializer {
    public static final String MOD_ID = "eat";
    @Override public void onInitialize() {
        dev.flower.eat.game.EatConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("eat.json"));
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        dev.flower.eat.game.OptionalFeedingBridges.install();
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, target, hit) ->
                dev.flower.eat.game.PlayerFeeding.interact(player, target, hand));
    }
}
