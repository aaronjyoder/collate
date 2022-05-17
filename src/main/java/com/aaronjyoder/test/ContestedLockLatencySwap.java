package com.aaronjyoder.test;

import com.aaronjyoder.CoreLatencyTest;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import net.openhft.affinity.AffinityLock;

public class ContestedLockLatencySwap implements CoreLatencyTest {

  private final long iterations;

  public ContestedLockLatencySwap(final long iterations) {
    this.iterations = iterations;
  }

  @Override
  public double latencyNanos(int threadA, int threadB) {
    Instant start, end;
    double result;

    AtomicLong bounce = new AtomicLong(0);
    long startA = 1;
    long startB = 2;

    var tB = new Thread(() -> {
      try (AffinityLock lock = AffinityLock.acquireLock(threadB)) {

        long current = startB;
        while (current < iterations) {
          while (true) {
            if (bounce.compareAndSet(current - 1, current)) {
              current += 2;
              break;
            }
          }
        }
      }
    });
    tB.setDaemon(true);
    tB.start();

    try (AffinityLock lock = AffinityLock.acquireLock(threadA)) {

      start = Instant.now();
      long current = startA;
      while (current < iterations) {
        while (true) {
          if (bounce.compareAndSet(current - 1, current)) {
            current += 2;
            break;
          }
        }
      }
      end = Instant.now();

      Duration duration = Duration.between(start, end);
      double avgNanosRoundTrip = (duration.toNanos() / ((double) iterations));
      result = avgNanosRoundTrip;
    }
    return result;
  }

}
