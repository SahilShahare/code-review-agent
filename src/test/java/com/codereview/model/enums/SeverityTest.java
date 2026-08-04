package com.codereview.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeverityTest {
  @Test
  void severityHasExactlyThreeLevelsInDeclaredOrder() {
    Severity[] values = Severity.values();

    assertEquals(3, values.length);
    assertEquals(Severity.BLOCKER, values[0]);
    assertEquals(Severity.WARNING, values[1]);
    assertEquals(Severity.NIT, values[2]);
  }

  @Test
  void severityValueOfIsCaseSensitive() {
    assertEquals(Severity.BLOCKER, Severity.valueOf("BLOCKER"));
  }
}
