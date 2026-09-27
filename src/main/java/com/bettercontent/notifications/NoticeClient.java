package com.bettercontent.notifications;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentNotifications.MOD_ID, value = Dist.CLIENT)
public final class NoticeClient {
    private static final int THREAD_GOLD = 0xC6A15B;
    private static final int COMBAT_RED = 0xC73E42;
    private static final int COMBAT_DARK = 0x2A080D;
    private static final NoticeQueue QUEUE = new NoticeQueue();
    private static long lastLiveFrame;

    private NoticeClient() {}

    static void receive(GameplayNotice notice) {
        QUEUE.add(notice);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        QUEUE.clear();
        lastLiveFrame = 0;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) {
            lastLiveFrame = 0;
            return;
        }
        long now = System.currentTimeMillis();
        long delta = lastLiveFrame == 0 ? 0 : Math.min(100, Math.max(0, now - lastLiveFrame));
        lastLiveFrame = now;
        var frame = QUEUE.advance(delta);
        if (frame == null) return;
        if (frame.started()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN,
                    frame.notice().theme() == NoticeTheme.THREADS ? 0.72f : 0.60f,
                    frame.notice().theme() == NoticeTheme.THREADS ? 0.38f : 0.64f));
        }
        renderNotice(event.getGuiGraphics(), frame.notice(), frame.elapsedMs(),
                event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    private static void renderNotice(GuiGraphics graphics, GameplayNotice notice, long elapsed,
            int screenWidth, int screenHeight) {
        float alpha = alpha(elapsed);
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 3;
        boolean combat = notice.theme() == NoticeTheme.COMBAT;
        particles(graphics, notice.identity().hashCode(), elapsed, alpha,
                centerX, centerY - 5, notice.accentRgb(),
                combat ? COMBAT_DARK : THREAD_GOLD);
        glyph(graphics, centerX - 4, centerY - 8, combat, alpha);
        drawText(graphics, notice.title(), centerX, centerY + 2,
                Math.max(0.55f, Math.min(0.72f,
                        (screenWidth - 24.0f) / Math.max(1, Minecraft.getInstance().font.width(notice.title())))),
                alpha, combat ? 0xF5B7B7 : 0xFFFFFF, combat ? 0x110307 : 0x000000);
        drawText(graphics, notice.hint(), centerX, centerY + 10,
                Math.min(0.58f, (screenWidth - 24.0f) / Math.max(1, Minecraft.getInstance().font.width(notice.hint()))),
                alpha * 0.82f, combat ? 0xE98185 : 0xFFFFFF, combat ? 0x110307 : 0x000000);
    }

    static float alpha(long elapsed) {
        if (elapsed < 400) return elapsed / 400.0f;
        if (elapsed < 2_600) return 1.0f;
        return Math.max(0, (NoticeQueue.DURATION_MS - elapsed) / 600.0f);
    }

    private static void glyph(GuiGraphics graphics, int x, int y, boolean combat, float alpha) {
        int[][] rows = combat
                ? new int[][] {{0, 7}, {1, 6}, {2, 5}, {3, 4}, {3, 4}, {2, 5}, {1, 6}, {0, 7}}
                : new int[][] {{3, 4}, {2, 5}, {1, 3, 4, 6}, {0, 2, 5, 7},
                        {0, 2, 5, 7}, {1, 3, 4, 6}, {2, 5}, {3, 4}};
        int color = (Math.round(alpha * 255) << 24) | (combat ? COMBAT_RED : THREAD_GOLD);
        for (int py = 0; py < rows.length; py++) {
            for (int px : rows[py]) graphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
        }
    }

    private static void drawText(GuiGraphics graphics, Component text, int x, int y,
            float scale, float alpha, int foreground, int outline) {
        int width = Minecraft.getInstance().font.width(text);
        int a = Math.round(alpha * 255) << 24;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1);
        for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) {
            if (ox != 0 || oy != 0) graphics.drawString(Minecraft.getInstance().font, text,
                    -width / 2 + ox, oy, a | outline, false);
        }
        graphics.drawString(Minecraft.getInstance().font, text, -width / 2, 0,
                a | foreground, false);
        pose.popPose();
    }

    private static void particles(GuiGraphics graphics, int seed, long elapsed, float alpha,
            int centerX, int centerY, int primary, int secondary) {
        double progress = elapsed / (double) NoticeQueue.DURATION_MS;
        for (int i = 0; i < 20; i++) {
            int mixed = mix(seed + i * 0x9E3779B9);
            double angle = ((mixed & 0xFFFF) / 65535.0) * Math.PI * 2;
            double radius = 6.0 + ((mixed >>> 16) & 3) + progress * (4.0 + ((mixed >>> 20) & 3));
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * (6.0 + ((mixed >>> 16) & 3))
                    - progress * (6.0 + ((mixed >>> 24) & 3)));
            float pulse = (float) (0.58 + 0.42 * Math.sin(Math.PI * Math.min(1.0,
                    progress * 1.25 + (i % 4) * 0.06)));
            int particleAlpha = (int) (alpha * pulse * (i < 12 ? 150 : 190));
            graphics.fill(x, y, x + 1, y + 1,
                    (particleAlpha << 24) | (i < 12 ? primary : secondary));
        }
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ (value >>> 16);
    }
}
