package com.codereview.util;

import org.junit.jupiter.api.Test;

import static com.codereview.constants.Constants.BOLD;
import static com.codereview.constants.Constants.CYAN;
import static com.codereview.constants.Constants.GREEN;
import static com.codereview.constants.Constants.RED;
import static com.codereview.constants.Constants.RESET;
import static com.codereview.constants.Constants.YELLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TerminalColorsTest {
  @Test
  void highlightsBlockerWordInRed() {
    String result = TerminalColors.colorizeReview("This is a blocker issue");

    assertEquals("This is a " + RED + BOLD + "blocker" + RESET + " issue", result);
  }

  @Test
  void highlightsWarningWordInYellow() {
    String result = TerminalColors.colorizeReview("This is a warning");

    assertEquals("This is a " + YELLOW + BOLD + "warning" + RESET, result);
  }

  @Test
  void highlightsNitWordInCyan() {
    String result = TerminalColors.colorizeReview("Just a nit here");

    assertEquals("Just a " + CYAN + BOLD + "nit" + RESET + " here", result);
  }

  @Test
  void wordHighlightingIsCaseInsensitiveButPreservesOriginalCase() {
    String result = TerminalColors.colorizeReview("BLOCKER: fix this");

    assertEquals(RED + BOLD + "BLOCKER" + RESET + ": fix this", result);
  }

  @Test
  void doesNotMatchWordAsSubstringOfALongerWord() {
    String result = TerminalColors.colorizeReview("There were several blockers found");

    // "blockers" should not be treated as the standalone word "blocker".
    assertEquals("There were several blockers found", result);
    assertFalse(result.contains(RED));
  }

  @Test
  void highlightsEntireFileHeaderLine() {
    String result = TerminalColors.colorizeReview("=== File: Foo.java ===");

    assertEquals(GREEN + BOLD + "=== File: Foo.java ===" + RESET, result);
  }

  @Test
  void onlyLinesStartingWithFilePrefixAreHighlighted() {
    String review = "Some intro text\n=== File: Foo.java ===\nblocker found here";

    String result = TerminalColors.colorizeReview(review);
    String[] lines = result.split("\n", -1);

    assertEquals(3, lines.length);
    assertEquals("Some intro text", lines[0]);
    assertEquals(GREEN + BOLD + "=== File: Foo.java ===" + RESET, lines[1]);
    assertTrue(lines[2].contains(RED + BOLD + "blocker" + RESET));
  }

  @Test
  void textWithNoKeywordsIsUnchanged() {
    String text = "Nothing interesting to see here.";
    assertEquals(text, TerminalColors.colorizeReview(text));
  }
}
