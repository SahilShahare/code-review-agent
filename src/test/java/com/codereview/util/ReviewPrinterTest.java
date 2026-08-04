package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.records.Finding;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReviewPrinterTest {

  @Test
  void printsNothingButNoNewlinesForEmptyFindingsList() {
    String output = printAndCapture(List.of());
    assertEquals("", output);
  }

  @Test
  void printsFileHeaderAndFindingLocationAndMessage() {
    Finding finding = new Finding("Foo.java", Severity.BLOCKER, "line 10", "Fix this");

    String output = printAndCapture(List.of(finding));

    assertTrue(output.contains("Foo.java"));
    assertTrue(output.contains("BLOCKER"));
    assertTrue(output.contains("line 10"));
    assertTrue(output.contains("Fix this"));
  }

  @Test
  void groupsMultipleFindingsByFilePreservingEncounterOrder() {
    Finding a1 = new Finding("A.java", Severity.BLOCKER, "l1", "issue one");
    Finding b1 = new Finding("B.java", Severity.NIT, "l2", "issue two");
    Finding a2 = new Finding("A.java", Severity.WARNING, "l3", "issue three");

    String output = printAndCapture(List.of(a1, b1, a2));

    int indexA = output.indexOf("A.java");
    int indexB = output.indexOf("B.java");
    int indexOne = output.indexOf("issue one");
    int indexTwo = output.indexOf("issue two");
    int indexThree = output.indexOf("issue three");

    // A.java's header should come before B.java's header (first-seen order).
    assertTrue(indexA < indexB);
    // Both A.java findings should be grouped together, i.e. appear before B.java's finding.
    assertTrue(indexOne < indexB);
    assertTrue(indexThree > indexA);
    assertTrue(indexTwo > indexB);
  }

  @Test
  void differentSeveritiesProduceDifferentBadgeLabels() {
    Finding blocker = new Finding("A.java", Severity.BLOCKER, "l1", "m1");
    Finding warning = new Finding("A.java", Severity.WARNING, "l2", "m2");
    Finding nit = new Finding("A.java", Severity.NIT, "l3", "m3");

    String output = printAndCapture(List.of(blocker, warning, nit));

    assertTrue(output.contains("BLOCKER"));
    assertTrue(output.contains("WARNING"));
    assertTrue(output.contains("NIT"));
  }

  private String printAndCapture(List<Finding> findings) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
    ReviewPrinter.print(findings, out);
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
