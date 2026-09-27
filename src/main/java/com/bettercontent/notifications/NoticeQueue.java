package com.bettercontent.notifications;

import java.util.ArrayDeque;
import java.util.Deque;

final class NoticeQueue {
    static final long DURATION_MS = 3_200L;
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
        elapsedMs = Math.min(DURATION_MS, elapsedMs + Math.max(0L, deltaMs));
        if (elapsedMs >= DURATION_MS) {
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
