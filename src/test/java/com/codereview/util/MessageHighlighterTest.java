package com.codereview.util;

import org.junit.jupiter.api.Test;

import static com.codereview.constants.Constants.BOLD;
import static com.codereview.constants.Constants.FG_BRIGHT_BLUE;
import static com.codereview.constants.Constants.FG_BRIGHT_RED;
import static com.codereview.constants.Constants.FG_MAGENTA;
import static com.codereview.constants.Constants.RESET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MessageHighlighterTest {

  @Test
  void plainMessageWithNoTagsIsReturnedUnchanged() {
    String message = "This is a plain message with no markup.";
    assertEquals(message, MessageHighlighter.highlight(message));
  }

  @Test
  void highlightsClassTagWithMagenta() {
    String result = MessageHighlighter.highlight("<class>UserService</class> is unused");

    String expected = FG_MAGENTA + BOLD + "UserService" + RESET + " is unused";
    assertEquals(expected, result);
  }

  @Test
  void highlightsMethodTagWithBrightBlue() {
    String result = MessageHighlighter.highlight("Consider inlining <method>doWork</method>");

    String expected = "Consider inlining " + FG_BRIGHT_BLUE + BOLD + "doWork" + RESET;
    assertEquals(expected, result);
  }

  @Test
  void highlightsExceptionTagWithBrightRed() {
    String result =
        MessageHighlighter.highlight("May throw <exception>NullPointerException</exception>");

    String expected = "May throw " + FG_BRIGHT_RED + BOLD + "NullPointerException" + RESET;
    assertEquals(expected, result);
  }

  @Test
  void handlesMultipleDistinctTagsInOneMessage() {
    String result =
        MessageHighlighter.highlight(
            "<class>Foo</class> calls <method>bar</method> which throws <exception>IOException</exception>");

    assertEquals(
        FG_MAGENTA
            + BOLD
            + "Foo"
            + RESET
            + " calls "
            + FG_BRIGHT_BLUE
            + BOLD
            + "bar"
            + RESET
            + " which throws "
            + FG_BRIGHT_RED
            + BOLD
            + "IOException"
            + RESET,
        result);
  }

  @Test
  void nonGreedyMatchingTreatsTwoSameTypeTagsAsSeparateSpans() {
    String result = MessageHighlighter.highlight("<class>A</class> and <class>B</class>");

    String expected = FG_MAGENTA + BOLD + "A" + RESET + " and " + FG_MAGENTA + BOLD + "B" + RESET;
    assertEquals(expected, result);

    assertFalse(result.contains("A</class> and <class>B"));
  }

  @Test
  void unknownTagNameIsNotMatchedAtAll() {
    String message = "<foo>bar</foo>";
    assertEquals(message, MessageHighlighter.highlight(message));
  }

  @Test
  void mismatchedTagNamesAreNotMatched() {
    String message = "<class>Foo</method>";
    assertEquals(message, MessageHighlighter.highlight(message));
  }
}
