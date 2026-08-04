package com.codereview.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LanguageTest {

  @Test
  void javaLanguageHasExpectedNameAndExtension() {
    assertEquals("Java", Language.JAVA.getName());
    assertTrue(Language.JAVA.getExtensions().contains("java"));
  }
}
