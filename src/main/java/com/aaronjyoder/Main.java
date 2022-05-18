package com.aaronjyoder;

import com.aaronjyoder.test.ContestedLockLatencySwap;
import com.aaronjyoder.util.MoshiUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) {
    try {
      int threadCount = Runtime.getRuntime().availableProcessors();
      System.out.println("-- Collate: CPU Latency Testing --");
      System.out.println("Threads detected: " + threadCount + "\n");

      System.out.println("This program tests round-trip core-to-core latency using a contested lock test that utilizes atomic compare and set instructions.");

      TimeUnit.SECONDS.sleep(1);

      System.out.println("The latency test will now begin. It may take some time, so please be patient.");
      double[][] table = generateCoreLatencyTable(threadCount);
      System.out.println("The latency test has finished. Saving results to file in current directory.");
      saveToFile(table);
    } catch (InterruptedException e) {
      System.out.println("Program halted due to the main thread being interrupted while sleeping.");
      throw new RuntimeException(e);
    }
  }

  private static double[][] generateCoreLatencyTable(int threadCount) throws InterruptedException {
    double[][] result = new double[threadCount][threadCount];
    for (int threadA = 0; threadA < threadCount; threadA++) {
      System.out.println("Testing thread " + threadA + " with all other threads.");
      for (int threadB = 0; threadB < threadCount; threadB++) {
        if (threadA != threadB) {
          System.out.print("\rCurrently testing thread " + threadA + " with thread " + threadB + "...");
          double latency = new ContestedLockLatencySwap(100_000_000L).latencyNanos(threadA, threadB);
          result[threadA][threadB] = latency;
        } else {
          result[threadA][threadB] = -1L;
        }
      }
      System.out.println("\rTests for thread " + threadA + " complete.\n");
      TimeUnit.SECONDS.sleep(1);
    }
    return result;
  }

  private static void saveToFile(double[][] table) {
    try {
      Path dir = Path.of("").toAbsolutePath();
      Path jsonPath = dir.resolve("collate-results.json");
      Path csvPath = dir.resolve("collate-results.csv");

      Files.createDirectories(dir); // Hopefully not necessary but just in case

      System.out.println("Results saved to file in current directory: 'collate-results.csv' and 'collate-results.json'");

      // Json
      MoshiUtil.write(jsonPath, table.getClass(), table);

      // CSV
      Files.writeString(csvPath, createCsv(table));
    } catch (IOException e) {
      System.out.println("Failed to write results to file.");
      e.printStackTrace();
    }
  }

  private static String createCsv(double[][] table) {
    StringBuilder result = new StringBuilder();
    for (int x = 0; x < table.length; x++) {
      for (int y = 0; y < table[x].length; y++) {
        if (table[x][y] == -1) {
          result.append("X");
        } else {
          result.append(table[x][y]);
        }
        if (y == table[x].length - 1) {
          result.append("\n");
        } else {
          result.append(", ");
        }
      }
    }
    return result.toString();
  }

}