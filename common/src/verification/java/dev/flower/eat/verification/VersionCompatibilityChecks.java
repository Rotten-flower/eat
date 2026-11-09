package dev.flower.eat.verification;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

/** Development actor with a connection context; no socket or real player inventory. */
final class VersionCompatibilityChecks {
    static ServerPlayer onlineOwner(ServerLevel level, GameProfile profile) throws Exception {
        ServerPlayer actor = null;
        for (var ctor : ServerPlayer.class.getConstructors()) {
            if (ctor.getParameterCount() == 3)
                actor = (ServerPlayer)ctor.newInstance(level.getServer(), level, profile);
            if (ctor.getParameterCount() == 4) {
                Object info = ctor.getParameterTypes()[3].getMethod("createDefault").invoke(null);
                actor = (ServerPlayer)ctor.newInstance(level.getServer(), level, profile, info);
            }
        }
        if (actor == null) throw new AssertionError("Missing native server player constructor");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        Class<?> listener = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
        for (var ctor : listener.getConstructors()) {
            Object handler;
            if (ctor.getParameterCount() == 3) handler = ctor.newInstance(level.getServer(), connection, actor);
            else if (ctor.getParameterCount() == 4) {
                Object cookie = ctor.getParameterTypes()[3].getMethod("createInitial", GameProfile.class, boolean.class).invoke(null, profile, false);
                handler = ctor.newInstance(level.getServer(), connection, actor, cookie);
            } else continue;
            ServerPlayer.class.getField("connection").set(actor, handler);
            return actor;
        }
        throw new AssertionError("Missing native server listener constructor");
    }
}
