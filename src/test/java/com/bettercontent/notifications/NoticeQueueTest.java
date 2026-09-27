package com.bettercontent.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class NoticeQueueTest {
    @Test
    void noticesFromBothThemesShareOneQueueWithoutDuplicates() {
        var queue = new NoticeQueue();
        var thread = new GameplayNotice("thread:one:1", NoticeTheme.THREADS,
                Component.literal("Card discovered"), Component.literal("Open Threads"));
        var combat = new GameplayNotice("style:sword", NoticeTheme.COMBAT,
                Component.literal("Sword learned"), Component.literal("Open Fighting Styles"));
        queue.add(thread);
        queue.add(thread);
        queue.add(combat);
        assertTrue(queue.advance(0).started());
        assertEquals(thread, queue.advance(300).notice());
        assertNull(queue.advance(NoticeQueue.DURATION_MS));
        assertEquals(combat, queue.advance(0).notice());
        assertFalse(queue.advance(1).started());
        queue.clear();
        assertNull(queue.advance(0));
    }
}
