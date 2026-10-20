package com.bettercontent.gameplaynotices;

import com.bettercontent.gameplaynotices.diagnostics.PlaytestLog;
import java.util.ArrayDeque;
import java.util.Deque;

final class NoticeQueue {
    static final long COMBAT_DURATION_MS = 3_200L;
    static final long THREAD_DURATION_MS = 8_000L;
    static long duration(GameplayNotice notice) {
        return notice.theme() == NoticeTheme.THREADS ? THREAD_DURATION_MS : COMBAT_DURATION_MS;
    }
    record Frame(GameplayNotice notice, long elapsedMs, boolean started) {}

    private final Deque<GameplayNotice> queue = new ArrayDeque<>();
    private final PlaytestLog diagnostics;

    NoticeQueue() { this(BetterGameplayNotices.PLAYTEST); }
    NoticeQueue(PlaytestLog diagnostics) { this.diagnostics = diagnostics; }
    private long elapsedMs;
    private boolean announced;

    /** Observes the existing admission decision; does not retain a parallel identity history. */
    boolean add(GameplayNotice notice) {
        if (queue.stream().noneMatch(existing -> existing.identity().equals(notice.identity()))) {
            queue.addLast(notice);
            outcome("notice.queued", "accepted", "new_identity", notice);
            return true;
        }
        outcome("notice.queued", "rejected", "duplicate_identity", notice);
        return false;
    }

    int size() {
        return queue.size();
    }

    Frame advance(long deltaMs) {
        if (queue.isEmpty()) return null;
        boolean started = !announced;
        announced = true;
        long duration = duration(queue.getFirst());
        elapsedMs = Math.min(duration, elapsedMs + Math.max(0L, deltaMs));
        if (elapsedMs >= duration) {
            GameplayNotice expired = queue.removeFirst();
            outcome("notice.expired", "removed", "duration_elapsed", expired);
            elapsedMs = 0L;
            announced = false;
            return null;
        }
        return new Frame(queue.getFirst(), elapsedMs, started);
    }

    int clear() {
        int removed = queue.size();
        queue.clear();
        elapsedMs = 0L;
        announced = false;
        if (removed > 0 && diagnostics.enabled("queue")) {
            diagnostics.event("queue", "notice.cleared", "client", "removed", "logout",
                    () -> PlaytestLog.fields("removed", removed));
        }
        return removed;
    }

    private void outcome(String event, String result, String reason, GameplayNotice notice) {
        if (diagnostics.enabled("queue")) {
            diagnostics.event("queue", event, "client", result, reason,
                    () -> PlaytestLog.fields("theme", notice.theme(), "queued", queue.size()));
        }
    }
}
