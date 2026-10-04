package com.bettercontent.gameplaynotices;

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
    private long elapsedMs;
    private boolean announced;

    void add(GameplayNotice notice) {
        if (queue.stream().noneMatch(existing -> existing.identity().equals(notice.identity()))) {
            queue.addLast(notice);
        }
    }

    Frame advance(long deltaMs) {
        if (queue.isEmpty()) return null;
        boolean started = !announced;
        announced = true;
        long duration = duration(queue.getFirst());
        elapsedMs = Math.min(duration, elapsedMs + Math.max(0L, deltaMs));
        if (elapsedMs >= duration) {
            queue.removeFirst();
            elapsedMs = 0L;
            announced = false;
            return null;
        }
        return new Frame(queue.getFirst(), elapsedMs, started);
    }

    void clear() {
        queue.clear();
        elapsedMs = 0L;
        announced = false;
    }
}
