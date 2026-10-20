package com.bettercontent.gameplaynotices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.gameplaynotices.diagnostics.PlaytestLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
        assertTrue(queue.add(thread));
        assertFalse(queue.add(thread));
        assertTrue(queue.add(combat));
        assertEquals(2, queue.size());
        assertTrue(queue.advance(0).started());
        assertEquals(thread, queue.advance(300).notice());
        assertEquals(thread, queue.advance(NoticeQueue.COMBAT_DURATION_MS).notice());
        assertNull(queue.advance(NoticeQueue.THREAD_DURATION_MS));
        assertEquals(combat, queue.advance(0).notice());
        assertEquals(combat, queue.advance(NoticeQueue.COMBAT_DURATION_MS - 1).notice());
        assertNull(queue.advance(1));
        queue.add(combat);
        assertEquals(combat, queue.advance(0).notice());
        assertFalse(queue.advance(1).started());
        assertEquals(1, queue.clear());
        assertEquals(0, queue.size());
        assertEquals(0, queue.clear());
        assertNull(queue.advance(0));
        assertTrue(queue.add(combat));
        assertTrue(queue.advance(0).started());
    }

    @Test
    void diagnosticsDescribeRealQueueOutcomesWithoutPrivateIdentityOrText() {
        List<String> records = new ArrayList<>();
        PlaytestLog.Sink sink = new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return true; }
            public void write(String category, String level, String message) { records.add(message); }
        };
        var queue = new NoticeQueue(new PlaytestLog("better_gameplay_notices", sink, () -> 0, new Properties()));
        var notice = new GameplayNotice("private-caller-identity", NoticeTheme.THREADS,
                Component.literal("private-title"), Component.literal("private-hint"));
        assertTrue(queue.add(notice));
        assertFalse(queue.add(notice));
        assertTrue(queue.advance(0).started());
        // Repeated render advances are not records.
        for (int i = 0; i < 100; i++) queue.advance(1);
        assertEquals(2, records.size());
        assertNull(queue.advance(NoticeQueue.THREAD_DURATION_MS));
        assertEquals(3, records.size());
        assertTrue(records.get(0).contains("result=accepted"));
        assertTrue(records.get(1).contains("reason=duplicate_identity"));
        assertTrue(records.get(2).contains("event=notice.expired"));
        assertTrue(records.stream().noneMatch(line -> line.contains("private-")));
    }

    @Test
    void disabledDiagnosticsPreserveQueueDecisionsAndTiming() {
        PlaytestLog.Sink disabled = new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return false; }
            public void write(String category, String level, String message) { throw new AssertionError("disabled sink"); }
        };
        var queue = new NoticeQueue(new PlaytestLog("better_gameplay_notices", disabled, () -> 0, new Properties()));
        var notice = new GameplayNotice("one", NoticeTheme.COMBAT, Component.literal("t"), Component.literal("h"));
        assertTrue(queue.add(notice));
        assertFalse(queue.add(notice));
        assertEquals(new NoticeQueue.Frame(notice, 0, true), queue.advance(0));
        assertEquals(new NoticeQueue.Frame(notice, 3_199, false), queue.advance(3_199));
        assertNull(queue.advance(1));
        assertEquals(0, queue.clear());
    }

    @Test
    void fadeTracksEachThemeDuration() {
        assertEquals(1.0f, NoticeClient.alpha(7_399, NoticeQueue.THREAD_DURATION_MS));
        assertEquals(0.5f, NoticeClient.alpha(7_700, NoticeQueue.THREAD_DURATION_MS));
        assertEquals(0.0f, NoticeClient.alpha(8_000, NoticeQueue.THREAD_DURATION_MS));
        assertEquals(0.5f, NoticeClient.alpha(2_900, NoticeQueue.COMBAT_DURATION_MS));
    }
}
