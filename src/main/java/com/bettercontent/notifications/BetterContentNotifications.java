package com.bettercontent.notifications;

import net.minecraftforge.fml.common.Mod;

@Mod(BetterContentNotifications.MOD_ID)
public final class BetterContentNotifications {
    public static final String MOD_ID = "better_content_notifications";

    public BetterContentNotifications() {
        NoticeNetwork.register();
    }
}
