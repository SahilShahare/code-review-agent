package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.records.Finding;
import com.codereview.model.records.ParseResult;
import com.codereview.model.records.UnparsedBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReviewParserTest {

  @Test
  void parsesSingleWellFormedBlock() {
    String raw =
        """
                FILE: src/main/java/Foo.java
                SEVERITY: BLOCKER
                LOCATION: line 42
                MESSAGE: Null pointer risk here
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertTrue(result.unparsed().isEmpty());

    Finding finding = result.findings().get(0);
    assertEquals("src/main/java/Foo.java", finding.file());
    assertEquals(Severity.BLOCKER, finding.severity());
    assertEquals("line 42", finding.location());
    assertEquals("Null pointer risk here", finding.message());
  }

  @Test
  void locationDefaultsToEmptyStringWhenAbsent() {
    String raw =
        """
                FILE: src/main/java/Foo.java
                SEVERITY: NIT
                MESSAGE: Minor style nit
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertEquals("", result.findings().get(0).location());
  }

  @Test
  void severityValueIsCaseInsensitive() {
    String raw =
        """
                FILE: src/main/java/Foo.java
                SEVERITY: warning
                MESSAGE: Consider renaming this variable
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertEquals(Severity.WARNING, result.findings().get(0).severity());
  }

  @Test
  void messageWrapsAcrossMultipleLinesUntilNextFieldOrBlockEnd() {
    String raw =
        """
                FILE: src/main/java/Baz.java
                SEVERITY: NIT
                MESSAGE: This is a message
                that wraps across
                multiple lines
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertEquals(
        "This is a message that wraps across multiple lines", result.findings().get(0).message());
  }

  @Test
  void splitsMultipleBlocksOnTripleDashSeparator() {
    String raw =
        """
                FILE: src/A.java
                SEVERITY: BLOCKER
                MESSAGE: first issue
                ---
                FILE: src/B.java
                SEVERITY: NIT
                MESSAGE: second issue
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(2, result.findings().size());
    assertEquals("src/A.java", result.findings().get(0).file());
    assertEquals("src/B.java", result.findings().get(1).file());
  }

  @Test
  void ignoresBlankSegmentsAroundLeadingAndTrailingSeparators() {
    String raw = "---\nFILE: src/A.java\nSEVERITY: NIT\nMESSAGE: msg\n---\n";

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertTrue(result.unparsed().isEmpty());
  }

  @Test
  void separatorRequiresAtLeastThreeDashesOnOwnLine() {
    // Only two dashes -- should NOT be treated as a separator, so this stays one block
    // and fails to parse as a single Finding (two FILE fields collide, second wins,
    // but severity/message are still present so it still parses as one finding).
    String raw =
        """
                FILE: src/A.java
                SEVERITY: BLOCKER
                MESSAGE: first
                --
                FILE: src/B.java
                SEVERITY: NIT
                MESSAGE: second
                """;

    ParseResult result = ReviewParser.parse(raw);

    // Treated as a single block since "--" doesn't match the separator pattern.
    assertEquals(1, result.findings().size());
    assertTrue(result.unparsed().isEmpty());
  }

  @Test
  void blockWithInvalidSeverityIsUnparsed() {
    String raw =
        """
                FILE: src/A.java
                SEVERITY: CRITICAL
                MESSAGE: bad severity value
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertTrue(result.findings().isEmpty());
    assertEquals(1, result.unparsed().size());
    assertTrue(result.unparsed().get(0).rawText().contains("CRITICAL"));
  }

  @Test
  void blockMissingFileIsUnparsed() {
    String raw =
        """
                SEVERITY: NIT
                MESSAGE: no file given
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertTrue(result.findings().isEmpty());
    assertEquals(1, result.unparsed().size());
  }

  @Test
  void blockMissingMessageIsUnparsed() {
    String raw =
        """
                FILE: src/A.java
                SEVERITY: NIT
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertTrue(result.findings().isEmpty());
    assertEquals(1, result.unparsed().size());
  }

  @Test
  void emptyInputProducesNoFindingsAndNoUnparsedBlocks() {
    ParseResult result = ReviewParser.parse("");

    assertTrue(result.findings().isEmpty());
    assertTrue(result.unparsed().isEmpty());
  }

  @Test
  void mixOfValidAndInvalidBlocksAreBothCaptured() {
    String raw =
        """
                FILE: src/A.java
                SEVERITY: BLOCKER
                MESSAGE: valid finding
                ---
                This block has no recognizable fields at all.
                """;

    ParseResult result = ReviewParser.parse(raw);

    assertEquals(1, result.findings().size());
    assertEquals(1, result.unparsed().size());

    List<UnparsedBlock> unparsed = result.unparsed();
    assertTrue(unparsed.get(0).rawText().contains("no recognizable fields"));
  }
}
