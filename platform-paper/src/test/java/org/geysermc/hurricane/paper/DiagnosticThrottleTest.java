package org.geysermc.hurricane.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class DiagnosticThrottleTest {
    @Test
    void rejectsNonPositiveIntervals() {
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticThrottle(0));
    }

    @Test
    void allowsOnlyAfterTheConfiguredInterval() {
        DiagnosticThrottle throttle = new DiagnosticThrottle(10);

        assertFalse(throttle.tryAcquire(9));
        assertTrue(throttle.tryAcquire(10));
        assertFalse(throttle.tryAcquire(19));
        assertTrue(throttle.tryAcquire(20));
    }

    @Test
    void allowsOnlyOneConcurrentCallerForAnInterval() throws Exception {
        DiagnosticThrottle throttle = new DiagnosticThrottle(10);
        int workers = 16;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < workers; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return throttle.tryAcquire(10);
                }));
            }
            ready.await();
            start.countDown();

            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }
    }
}
