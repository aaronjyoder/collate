package com.aaronjyoder.util;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MoshiUtil {

  private MoshiUtil() {
  }

  private static final Moshi.Builder jsonAdapterBuilder = new Moshi.Builder();

  private static Moshi jsonAdapter() {
    return jsonAdapterBuilder.build();
  }

  // Read

  public static <T> T read(Path path, Class<T> type) throws IOException {
    if (Files.isRegularFile(path) && Files.isReadable(path)) {
      JsonAdapter<T> jsonAdapter = jsonAdapter().adapter(type);
      return jsonAdapter.fromJson(Files.readString(path));
    }
    return null;
  }

  public static <T> T read(Path path, Type type) throws IOException {
    if (Files.isRegularFile(path) && Files.isReadable(path)) {
      JsonAdapter<T> jsonAdapter = jsonAdapter().adapter(type);
      return jsonAdapter.fromJson(Files.readString(path));
    }
    return null;
  }

  // Write

  public static <T> void write(Path path, Class<T> type, T object) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, jsonAdapter().adapter(type).indent("  ").toJson(object), StandardCharsets.UTF_8);
  }

  public static <T> void write(Path path, Type type, T object) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, jsonAdapter().adapter(type).indent("  ").toJson(object), StandardCharsets.UTF_8);
  }

}