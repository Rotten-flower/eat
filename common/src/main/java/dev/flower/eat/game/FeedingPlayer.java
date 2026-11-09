package dev.flower.eat.game;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/** 未注册到世界的交互上下文，不修改真实玩家背包。 Unregistered interaction context that never changes a real player’s inventory. */
public final class FeedingPlayer extends Player {
    private static final GameProfile UNATTRIBUTED = new GameProfile(new UUID(0L, 0L), "eat");
    private GameProfile feedingProfile = UNATTRIBUTED;

    public FeedingPlayer(ServerLevel level) {
        super(level, BlockPos.ZERO, 0F, UNATTRIBUTED);
    }

    public void prepare(GameProfile profile, double x, double y, double z) {
        feedingProfile = profile == null ? UNATTRIBUTED : profile;
        setUUID(feedingProfile.getId());
        setPos(x, y, z);
        getInventory().clearContent();
    }

    @Override public GameProfile getGameProfile() { return feedingProfile; }
    @Override public boolean isSpectator() { return false; }
    @Override public boolean isCreative() { return false; }
}
