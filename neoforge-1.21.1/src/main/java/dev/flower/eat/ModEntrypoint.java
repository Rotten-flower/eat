package dev.flower.eat;
import net.neoforged.fml.common.Mod;
import dev.flower.eat.core.ModIdentity;
@Mod(ModEntrypoint.MOD_ID)
public final class ModEntrypoint {
    public static final String MOD_ID = "eat";
    public ModEntrypoint() {
        dev.flower.eat.game.EatConfig.load(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("eat.json"));
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, this::feedPlayer);
        dev.flower.eat.game.OptionalFeedingBridges.install();
    }
    private void feedPlayer(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        var result = dev.flower.eat.game.PlayerFeeding.interact(event.getEntity(), event.getTarget(), event.getHand());
        if (result != net.minecraft.world.InteractionResult.PASS) { event.setCancellationResult(result); event.setCanceled(true); }
    }
}
