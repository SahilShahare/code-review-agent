package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.records.Finding;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.codereview.constants.Constants.BG_CYAN;
import static com.codereview.constants.Constants.BG_RED;
import static com.codereview.constants.Constants.BG_YELLOW;
import static com.codereview.constants.Constants.BOLD;
import static com.codereview.constants.Constants.FG_BLACK;
import static com.codereview.constants.Constants.FG_GREEN;
import static com.codereview.constants.Constants.FG_WHITE;
import static com.codereview.constants.Constants.RESET;

public class ReviewPrinter {

  private ReviewPrinter() {}

  public static void print(List<Finding> findings, PrintStream out) {
    Map<String, List<Finding>> byFile =
        findings.stream()
            .collect(Collectors.groupingBy(Finding::file, LinkedHashMap::new, Collectors.toList()));

    for (var entry : byFile.entrySet()) {
      String file = entry.getKey();
      out.println();
      out.println(
          BOLD
              + FG_GREEN
              + "── "
              + file
              + " "
              + "─".repeat(Math.max(4, 70 - file.length()))
              + RESET);

      for (Finding f : entry.getValue()) {
        out.println(badge(f.severity()) + " " + BOLD + f.location() + RESET);
        out.println("   " + MessageHighlighter.highlight(f.message()));
        out.println();
      }
    }
  }

  private static String badge(Severity severity) {
    return switch (severity) {
      case BLOCKER -> BG_RED + FG_WHITE + BOLD + " BLOCKER " + RESET;
      case WARNING -> BG_YELLOW + FG_BLACK + BOLD + " WARNING " + RESET;
      case NIT -> BG_CYAN + FG_BLACK + BOLD + "   NIT   " + RESET;
    };
  }
}
