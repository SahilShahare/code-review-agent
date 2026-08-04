package com.codereview.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codereview.constants.Constants.BOLD;
import static com.codereview.constants.Constants.FG_BRIGHT_BLUE;
import static com.codereview.constants.Constants.FG_BRIGHT_RED;
import static com.codereview.constants.Constants.FG_MAGENTA;
import static com.codereview.constants.Constants.RESET;

public class MessageHighlighter {

  // Non-greedy .*? so "<class>A</class> and <class>B</class>" matches as
  // two separate tags instead of one span from the first <class> to the last </class>.
  private static final Pattern TAG = Pattern.compile("<(class|method|exception)>(.*?)</\\1>");

  private MessageHighlighter() {}

  public static String highlight(String message) {
    Matcher m = TAG.matcher(message);
    StringBuilder sb = new StringBuilder();

    while (m.find()) {
      String color =
          switch (m.group(1)) {
            case "class" -> FG_MAGENTA;
            case "method" -> FG_BRIGHT_BLUE;
            case "exception" -> FG_BRIGHT_RED;
            default -> "";
          };
      m.appendReplacement(sb, Matcher.quoteReplacement(color + BOLD + m.group(2) + RESET));
    }
    m.appendTail(sb);

    return sb.toString();
  }
}
