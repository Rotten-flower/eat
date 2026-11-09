package dev.flower.eat.mixin;

import dev.flower.eat.game.FeedingPlayer;
import dev.flower.eat.game.FeedingWorld;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements FeedingWorld {
    @Unique private FeedingPlayer eat$feedingPlayer;

    @Override public FeedingPlayer eat$getFeedingPlayer() {
        if (eat$feedingPlayer == null) eat$feedingPlayer = new FeedingPlayer((ServerLevel)(Object)this);
        return eat$feedingPlayer;
    }
}
