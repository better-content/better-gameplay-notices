package com.bettercontent.gameplaynotices;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** Public entry point for server-owned notices from feature mods. */
public final class GameplayNotices {
    private GameplayNotices() {}

    public static void send(ServerPlayer player, GameplayNotice notice) {
        NoticeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), notice);
    }
}
