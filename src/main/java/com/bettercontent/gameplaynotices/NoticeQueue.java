package com.bettercontent.gameplaynotices;

import com.bettercontent.gameplaynotices.diagnostics.PlaytestLog;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

final class NoticeQueue {
    static final long COMBAT_DURATION_MS = 3_200L;
    static final long THREAD_DURATION_MS = 8_000L;
    static final long TRACE_DURATION_MS = 3_200L;
    static final long TRANSIENT_PENDING_MS = 5_000L;
    static long duration(GameplayNotice notice) {
        return switch (notice.theme()) {
            case THREADS -> THREAD_DURATION_MS;
            case COMBAT -> COMBAT_DURATION_MS;
            case TRACES -> TRACE_DURATION_MS;
        };
    }
    record Frame(GameplayNotice notice, long elapsedMs, boolean started) {}

    private final Deque<GameplayNotice> queue = new ArrayDeque<>();
    private final PlaytestLog diagnostics;

    private final LongSupplier clock;
    private Transient transientNotice;

    private static final class Transient {
        final String key;
        final GameplayNotice notice;
        final BooleanSupplier valid;
        final Runnable onShown;
        final long submittedAt;
        long elapsed;
        boolean shown;

        Transient(String key, GameplayNotice notice, BooleanSupplier valid, Runnable onShown, long submittedAt) {
            this.key = key;
            this.notice = notice;
            this.valid = valid;
            this.onShown = onShown;
            this.submittedAt = submittedAt;
        }
    }

    NoticeQueue() { this(BetterGameplayNotices.PLAYTEST); }
    NoticeQueue(PlaytestLog diagnostics) { this(diagnostics, () -> System.nanoTime() / 1_000_000L); }
    NoticeQueue(PlaytestLog diagnostics, LongSupplier clock) {
        this.diagnostics = diagnostics;
        this.clock = Objects.requireNonNull(clock);
    }

    void submitTransient(String key, GameplayNotice notice, BooleanSupplier valid, Runnable onShown) {
        Objects.requireNonNull(key);
        if (key.isBlank() || key.length() > 160) throw new IllegalArgumentException("Invalid transient key");
        transientNotice = new Transient(key, Objects.requireNonNull(notice), Objects.requireNonNull(valid),
                Objects.requireNonNull(onShown), clock.getAsLong());
        maintainTransient();
    }

    void cancelTransient(String key) {
        if (transientNotice != null && transientNotice.key.equals(key)) transientNotice = null;
    }

    /** Runs even while the HUD cannot present notices. Never starts a timer or callback. */
    void maintainTransient() {
        var pending = transientNotice;
        if (pending != null && ((!pending.shown && clock.getAsLong() - pending.submittedAt >= TRANSIENT_PENDING_MS)
                || !pending.valid.getAsBoolean())) transientNotice = null;
    }
    private long elapsedMs;
    private boolean announced;

    /** Observes the existing admission decision; does not retain a parallel identity history. */
    boolean add(GameplayNotice notice) {
        if (queue.stream().noneMatch(existing -> existing.identity().equals(notice.identity()))) {
            transientNotice = null;
            queue.addLast(notice);
            outcome("notice.queued", "accepted", "new_identity", notice);
            return true;
        }
        outcome("notice.queued", "rejected", "duplicate_identity", notice);
        return false;
    }

    int size() {
        return queue.size() + (transientNotice == null ? 0 : 1);
    }

    Frame advance(long deltaMs) {
        maintainTransient();
        if (queue.isEmpty()) return advanceTransient(deltaMs);
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

    private Frame advanceTransient(long deltaMs) {
        var current = transientNotice;
        if (current == null) return null;
        boolean started = !current.shown;
        if (started) {
            current.shown = true;
            current.onShown.run();
            // The callback may cancel, replace, or enqueue a discovery.
            if (transientNotice != current) return null;
        } else {
            current.elapsed = Math.min(duration(current.notice), current.elapsed + Math.max(0L, deltaMs));
        }
        if (current.elapsed >= duration(current.notice)) {
            transientNotice = null;
            return null;
        }
        return new Frame(current.notice, current.elapsed, started);
    }

    int clear() {
        int removed = size();
        queue.clear();
        transientNotice = null;
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
