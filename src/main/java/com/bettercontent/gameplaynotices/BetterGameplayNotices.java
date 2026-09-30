package com.bettercontent.gameplaynotices;

import net.minecraftforge.fml.common.Mod;

@Mod(BetterGameplayNotices.MOD_ID)
public final class BetterGameplayNotices {
    public static final String MOD_ID = "better_gameplay_notices";

    public BetterGameplayNotices() {
        NoticeNetwork.register();
    }
}
