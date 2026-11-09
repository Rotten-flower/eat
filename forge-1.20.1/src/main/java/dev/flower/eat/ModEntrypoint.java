package dev.flower.eat;
import net.minecraftforge.fml.common.Mod;
import dev.flower.eat.core.ModIdentity;
@Mod(ModEntrypoint.MOD_ID)
public final class ModEntrypoint {
    public static final String MOD_ID = "eat";
    public ModEntrypoint() {
        dev.flower.eat.game.EatConfig.load(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("eat.json"));
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, false, this::feedPlayer);
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }
    private void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> { dev.flower.eat.game.OptionalFeedingBridges.install(); MaidPickupBridge.install(); });
    }
    private void feedPlayer(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        var result = dev.flower.eat.game.PlayerFeeding.interact(event.getEntity(), event.getTarget(), event.getHand());
        if (result != net.minecraft.world.InteractionResult.PASS) { event.setCancellationResult(result); event.setCanceled(true); }
    }
}
