package com.codereview.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codereview.constants.Constants.BOLD;
import static com.codereview.constants.Constants.CYAN;
import static com.codereview.constants.Constants.GREEN;
import static com.codereview.constants.Constants.RED;
import static com.codereview.constants.Constants.RESET;
import static com.codereview.constants.Constants.YELLOW;

public class TerminalColors {

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
