package dev.flower.eat.mixin.client;

import dev.flower.eat.game.FedPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 接收者无需持续右键，主动攻击或潜行会中断进食。 The receiver need not hold right-click; attack or sneak stops eating. */
@Mixin(MultiPlayerGameMode.class)
public abstract class PlayerEatingControlMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void eat$continueReceivedFood(Player player, CallbackInfo ci) {
        if (!((FedPlayer)player).eat$isExternallyFed() || !player.isUsingItem()
                || !dev.flower.eat.game.VersionFood.playerFood(player.getUseItem())) return;
        var options = Minecraft.getInstance().options;
        if (!options.keyAttack.isDown() && !options.keyShift.isDown()) ci.cancel();
    }
}
