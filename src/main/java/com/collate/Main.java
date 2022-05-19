package com.collate;

import com.collate.test.LatencyTest;
import com.collate.test.cpu.ContestedLockLatencyTest;
import com.collate.util.MoshiUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import oshi.SystemInfo;

public class Main {

  public static void main(String[] args) {
    try {
      int threadCount = Runtime.getRuntime().availableProcessors();
      System.out.println("-- Collate: CPU Latency Testing --");
      System.out.println("Threads detected: " + threadCount);

      System.out.println("This program tests round-trip core-to-core latency using a contested lock test that utilizes atomic compare and set instructions.");
      System.out.println();

      // Latency test begins
      final long ITERATIONS = 1_000_000L;
      final int MAX_RETRIES = 5;
      System.out.println("The latency test will now begin. It may take some time, so please be patient.");
      TimeUnit.SECONDS.sleep(1);
      double[][] table = generateThreadLatencyTable(threadCount, new ContestedLockLatencyTest(ITERATIONS));
      for (int i = 0; i < MAX_RETRIES; i++) {
        if (!containsUnexpectedNegative(table)) {
          break;
        }
        System.out.println("The results contained an unexpected negative value, invalidating the results. Waiting 5 seconds, and then trying again.");
        System.out.println();
        TimeUnit.SECONDS.sleep(5);
        System.out.println("-- Retry " + (i + 1) + " of " + MAX_RETRIES + " --");
        table = generateThreadLatencyTable(threadCount, new ContestedLockLatencyTest(ITERATIONS));
      }
      if (containsUnexpectedNegative(table)) {
        System.out.println("The table still contains invalid results after 5 retries. Program exiting early. Results have not been saved.");
        return;
      }
      // Save to file
      System.out.println("The latency test has finished. Saving results to file in current directory.");
      saveToFile(table);
    } catch (InterruptedException e) {
      System.out.println("Program halted due to the main thread being interrupted while sleeping.");
      throw new RuntimeException(e);
    }
  }

  private static double[][] generateThreadLatencyTable(int threadCount, LatencyTest test) {
    double[][] result = new double[threadCount][threadCount];
    for (int threadA = 0; threadA < threadCount; threadA++) {
      System.out.println("Testing thread " + threadA + " with all other threads.");
      for (int threadB = 0; threadB < threadCount; threadB++) {
        if (threadA != threadB) {
          System.out.print("\rCurrently testing thread " + threadA + " with thread " + threadB + "...");
          double element = test.latencyNanos(threadA, threadB);
          result[threadA][threadB] = element;
        } else {
          result[threadA][threadB] = -1L;
        }
      }
      System.out.println("\rTests for thread " + threadA + " complete.\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\n");
    }
    return result;
  }

  private static boolean containsUnexpectedNegative(double[][] table) {
    for (int x = 0; x < table.length; x++) {
      for (int y = 0; y < table.length; y++) {
        if (x != y && table[x][y] < 0) {
          return true;
        }
      }
    }
    return false;
  }

  // TODO: Linux seems to sometimes sort the cores differently, so need to read from `grep -E 'processor|core id' /proc/cpuinfo` and sort before writing to file
  private static void saveToFile(double[][] table) {
    try {
      SystemInfo si = new SystemInfo();
      var hal = si.getHardware();
      var cpu = hal.getProcessor();
      String cpuName = cpu.getProcessorIdentifier().getName().toLowerCase()
          .replaceAll("processor", "").trim()
          .replaceAll("\s+", "\s")
          .replaceAll("\s", "-")
          .replaceAll("[^a-zA-Z\\d.\\-]", "");

      String fileName = "collate-" + cpuName + "-results";

      Path dir = Path.of("").toAbsolutePath();
      Path jsonPath = dir.resolve(fileName + ".json");
      Path csvPath = dir.resolve(fileName + ".csv");

      Files.createDirectories(dir); // Hopefully not necessary but just in case

      System.out.println("Results saved to file in current directory: '" + fileName + ".csv' and '" + fileName + ".json'");

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
