package com.bettercontent.gameplaynotices;

import com.bettercontent.gameplaynotices.diagnostics.PlaytestLog;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(BetterGameplayNotices.MOD_ID)
public final class BetterGameplayNotices {
    public static final String MOD_ID = "better_gameplay_notices";
    static final PlaytestLog PLAYTEST = new PlaytestLog(MOD_ID);

    public BetterGameplayNotices() {
        NoticeNetwork.register();
        PLAYTEST.status("common", ModList.get().getModContainerById(MOD_ID)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("UNKNOWN"), source(),
                () -> PlaytestLog.fields("enabled", true, "network_protocol", 2));
        MinecraftForge.EVENT_BUS.addListener(BetterGameplayNotices::serverTick);
        MinecraftForge.EVENT_BUS.addListener(BetterGameplayNotices::serverStopped);
    }

    private static String source() {
        var file = ModList.get().getModFileById(MOD_ID);
        return PLAYTEST.sourceIdentity(file == null || file.getFile() == null ? (java.nio.file.Path) null
                : file.getFile().findResource("META-INF", "better-content-source.properties"));
    }
    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && PLAYTEST.enabled("summary")) PLAYTEST.summary("server");
    }

    private static void serverStopped(ServerStoppedEvent event) { PLAYTEST.reset("server"); }
}
