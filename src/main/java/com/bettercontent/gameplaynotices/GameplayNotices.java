package com.bettercontent.gameplaynotices;

import com.bettercontent.gameplaynotices.diagnostics.PlaytestLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** Public entry point for server-owned notices from feature mods. */
public final class GameplayNotices {
    private GameplayNotices() {}

    public static void send(ServerPlayer player, GameplayNotice notice) {
        NoticeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), notice);
        if (BetterGameplayNotices.PLAYTEST.enabled("delivery")) {
            BetterGameplayNotices.PLAYTEST.event("delivery", "notice.sent", "server", "submitted", "channel_send_returned",
                    () -> PlaytestLog.fields("theme", notice.theme()));
        }
    }
}
