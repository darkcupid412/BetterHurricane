package org.geysermc.hurricane.paper;

import java.util.concurrent.atomic.AtomicLong;

final class DiagnosticThrottle {
    private final long intervalNanos;
    private final AtomicLong last = new AtomicLong();

    DiagnosticThrottle(long intervalNanos) {
        if (intervalNanos <= 0) {
            throw new IllegalArgumentException("intervalNanos must be positive");
        }
        this.intervalNanos = intervalNanos;
    }

    boolean tryAcquire(long now) {
        long previous = last.get();
        while (now - previous >= intervalNanos) {
            if (last.compareAndSet(previous, now)) {
                return true;
            }
            previous = last.get();
        }
        return false;
    }
}
