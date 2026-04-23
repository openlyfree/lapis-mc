package io.github.openlyfree.utils;

import java.util.ArrayList;
import java.util.List;

public class LogBuffer {
  private final int maxSize = 500;
  private final java.util.Queue<String> lines = new java.util.concurrent.ConcurrentLinkedQueue<>();

  public void add(String line) {
    lines.add(line);
    while (lines.size() > maxSize) {
      lines.poll();
    }
  }

  public List<String> getRecent() {
    return new ArrayList<String>(lines);
  }
}