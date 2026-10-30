package com.bettercontent.gameplaynotices;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class TransientNoticeTest {
    private final AtomicLong clock = new AtomicLong(100);
    private final NoticeQueue queue = new NoticeQueue(BetterGameplayNotices.PLAYTEST, clock::get);
    private final AtomicInteger shown = new AtomicInteger();

    private GameplayNotice notice(String id, NoticeTheme theme) {
        return new GameplayNotice(id, theme, Component.literal("Title"), Component.literal("Hint"));
    }

    private void submit(String key, GameplayNotice notice) {
        queue.submitTransient(key, notice, () -> true, shown::incrementAndGet);
    }

    @Test
    void firstPresentationStartsTimerAndCallbackNotAdmissionOrHiddenMaintenance() {
        var trace = notice("trace", NoticeTheme.TRACES);
        submit("nearby", trace);
        clock.addAndGet(4_999);
        queue.maintainTransient();
        assertEquals(0, shown.get());
        assertEquals(new NoticeQueue.Frame(trace, 0, true), queue.advance(100));
        assertEquals(1, shown.get());
        clock.addAndGet(20_000); // Pending expiry no longer applies after presentation.
        assertEquals(new NoticeQueue.Frame(trace, 3_199, false), queue.advance(3_199));
        assertNull(queue.advance(1));
        assertEquals(1, shown.get());
        assertEquals(0, queue.size());
    }

    @Test
    void pendingExpiresAtExactlyFiveSecondsEvenBehindDiscovery() {
        var discovery = notice("thread", NoticeTheme.THREADS);
        queue.add(discovery);
        submit("nearby", notice("trace", NoticeTheme.TRACES));
        assertEquals(2, queue.size());
        clock.addAndGet(5_000);
        queue.maintainTransient();
        assertEquals(1, queue.size());
        queue.advance(0);
        assertNull(queue.advance(8_000));
        assertNull(queue.advance(0));
        assertEquals(0, shown.get());
    }

    @Test
    void discoveryFifoWinsAndPendingTransientOnlyStartsAfterwardsIfFresh() {
        var first = notice("combat", NoticeTheme.COMBAT);
        var second = notice("thread", NoticeTheme.THREADS);
        var trace = notice("trace", NoticeTheme.TRACES);
        queue.add(first);
        queue.add(second);
        submit("nearby", trace);
        assertEquals(first, queue.advance(0).notice());
        assertNull(queue.advance(3_200));
        assertEquals(second, queue.advance(0).notice());
        assertNull(queue.advance(8_000));
        // Render elapsed and monotonic pending age are intentionally independent.
        assertEquals(new NoticeQueue.Frame(trace, 0, true), queue.advance(100));
        assertEquals(1, shown.get());
    }

    @Test
    void acceptedDiscoveryDiscardsVisibleOrPendingTransientWithoutReplay() {
        for (boolean visible : new boolean[] {false, true}) {
            queue.clear();
            submit("nearby", notice("trace", NoticeTheme.TRACES));
            if (visible) queue.advance(0);
            var discovery = notice("discovery", NoticeTheme.THREADS);
            assertTrue(queue.add(discovery));
            assertEquals(1, queue.size());
            assertEquals(discovery, queue.advance(0).notice());
            assertNull(queue.advance(8_000));
            assertNull(queue.advance(0));
        }
        assertEquals(1, shown.get());
    }

    @Test
    void invalidityCancelsPendingAndVisibleEvenWithoutRender() {
        var valid = new AtomicBoolean(true);
        for (boolean visible : new boolean[] {false, true}) {
            valid.set(true);
            queue.submitTransient("nearby", notice("trace", NoticeTheme.TRACES), valid::get, shown::incrementAndGet);
            if (visible) queue.advance(0);
            valid.set(false);
            queue.maintainTransient();
            assertEquals(0, queue.size());
            assertNull(queue.advance(0));
        }
        assertEquals(1, shown.get());
    }

    @Test
    void stableKeyReplacementIsBoundedAndCancellationCannotAffectOtherKeyOrDiscoveries() {
        var trace = notice("trace", NoticeTheme.TRACES);
        submit("old", trace);
        submit("new", trace);
        queue.cancelTransient("old");
        assertEquals(1, queue.size());
        queue.cancelTransient("new");
        assertNull(queue.advance(0));
        submit("same", trace);
        clock.addAndGet(4_999);
        var replacement = notice("replacement", NoticeTheme.TRACES);
        submit("same", replacement);
        clock.addAndGet(1);
        assertEquals(new NoticeQueue.Frame(replacement, 0, true), queue.advance(0));
        submit("same", trace); // Visible replacement is a new, not replayed presentation.
        assertEquals(new NoticeQueue.Frame(trace, 0, true), queue.advance(0));
        assertEquals(2, shown.get());
        var discovery = notice("same", NoticeTheme.THREADS);
        queue.add(discovery);
        queue.cancelTransient("same");
        assertEquals(discovery, queue.advance(0).notice());
    }

    @Test
    void logoutClearsSlotAndCallbacksCanCancelOrEnqueueWithoutStaleFrame() {
        submit("key", notice("trace", NoticeTheme.TRACES));
        assertEquals(1, queue.clear());
        assertNull(queue.advance(0));
        assertEquals(0, shown.get());
        queue.submitTransient("key", notice("trace", NoticeTheme.TRACES), () -> true,
                () -> queue.cancelTransient("key"));
        assertNull(queue.advance(0));
        var discovery = notice("thread", NoticeTheme.THREADS);
        queue.submitTransient("key", notice("trace", NoticeTheme.TRACES), () -> true,
                () -> queue.add(discovery));
        assertNull(queue.advance(0));
        assertEquals(discovery, queue.advance(0).notice());
    }

    @Test
    void allThemesHaveExplicitDefaultsAndDurationsAndDiscoveryDedupRemains() {
        assertEquals(0xB8D4DD, notice("trace", NoticeTheme.TRACES).accentRgb());
        assertEquals(0xC6A15B, notice("thread", NoticeTheme.THREADS).accentRgb());
        assertEquals(0xC73E42, notice("combat", NoticeTheme.COMBAT).accentRgb());
        assertEquals(3_200, NoticeQueue.duration(notice("trace", NoticeTheme.TRACES)));
        assertEquals(3_200, NoticeQueue.duration(notice("combat", NoticeTheme.COMBAT)));
        assertEquals(8_000, NoticeQueue.duration(notice("thread", NoticeTheme.THREADS)));
        var discovery = notice("thread", NoticeTheme.THREADS);
        queue.add(discovery);
        submit("nearby", notice("trace", NoticeTheme.TRACES));
        assertFalse(queue.add(discovery)); // Rejected duplicate is not a new discovery arrival.
        assertEquals(2, queue.size());
        assertThrows(IllegalArgumentException.class, () -> submit(" ", discovery));
    }
}
