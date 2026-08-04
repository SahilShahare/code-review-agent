package com.codereview.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TerminalColors {

  private static final String RESET = "\u001B[0m";
  private static final String BOLD = "\u001B[1m";
  private static final String RED = "\u001B[31m";
  private static final String YELLOW = "\u001B[33m";
  private static final String CYAN = "\u001B[36m";
  private static final String GREEN = "\u001B[32m";

  private TerminalColors() {}

  public static String colorizeReview(String review) {
    String result = review;
    result = highlightWord(result, "blocker", RED);
    result = highlightWord(result, "warning", YELLOW);
    result = highlightWord(result, "nit", CYAN);
    result = highlightLinePrefix(result, "=== File:", GREEN);
    return result;
  }

  private static String highlightWord(String text, String word, String color) {
    Pattern pattern =
        Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          sb, Matcher.quoteReplacement(color + BOLD + matcher.group() + RESET));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static String highlightLinePrefix(String text, String prefix, String color) {
    String[] lines = text.split("\n", -1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.startsWith(prefix)) {
        sb.append(color).append(BOLD).append(line).append(RESET);
      } else {
        sb.append(line);
      }
      if (i < lines.length - 1) sb.append("\n");
    }
    return sb.toString();
  }
}
