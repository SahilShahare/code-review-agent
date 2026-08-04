package com.codereview.factory;

import com.codereview.analyzer.AstAnalyzer;
import com.codereview.analyzer.JavaAstAnalyzer;
import com.codereview.model.enums.Language;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AstAnalyzerFactoryTest {
  @Test
  void buildReturnsJavaAstAnalyzerForJavaLanguage() {
    Optional<AstAnalyzer> analyzer = AstAnalyzerFactory.build(Language.JAVA);

    assertTrue(analyzer.isPresent());
    assertInstanceOf(JavaAstAnalyzer.class, analyzer.get());
  }

  @Test
  void allReturnsOneAnalyzerPerSupportedLanguage() {
    Set<AstAnalyzer> analyzers = AstAnalyzerFactory.all();

    assertEquals(Language.values().length, analyzers.size());
    assertTrue(analyzers.stream().anyMatch(a -> a instanceof JavaAstAnalyzer));
  }
}
