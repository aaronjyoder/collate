package com.collate;

public interface LatencyTest {

  double latencyNanos(int threadA, int threadB);

}
