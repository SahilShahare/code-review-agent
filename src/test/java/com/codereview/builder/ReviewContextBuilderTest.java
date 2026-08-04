package com.codereview.builder;

import com.codereview.model.CallGraph;
import com.codereview.model.records.JavaMethodSignature;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReviewContextBuilderTest {

  @Test
  void buildPromptIncludesDiffSectionForEachChangedFile() throws IOException {
    CallGraph callGraph = new CallGraph();
    ReviewContextBuilder builder = new ReviewContextBuilder(callGraph, "testPrompt.txt");

    Map<String, String> diffs = Map.of("src/Foo.java", "@@ -1,1 +1,2 @@\n+added line");

    String prompt = builder.buildPrompt(diffs);

    assertTrue(prompt.contains("TEST PROMPT START"));
    assertTrue(prompt.contains("=== File: src/Foo.java ==="));
    assertTrue(prompt.contains("@@ -1,1 +1,2 @@"));
    assertTrue(prompt.contains("+added line"));
  }

  @Test
  void buildPromptIncludesCallGraphSectionOnlyForMethodsInChangedFiles() throws IOException {
    CallGraph callGraph = new CallGraph();

    JavaMethodSignature changed = new JavaMethodSignature("com.example.Foo.bar()", "src/Foo.java");
    JavaMethodSignature calleeOutsideDiff =
        new JavaMethodSignature("com.example.Baz.qux()", "src/Baz.java");

    callGraph.registerMethod(changed, "public void bar() { qux(); }");
    callGraph.registerMethod(calleeOutsideDiff, "void qux() {}");
    callGraph.addCall(changed, calleeOutsideDiff);

    Map<String, String> diffs = Map.of("src/Foo.java", "diff for Foo");

    ReviewContextBuilder builder = new ReviewContextBuilder(callGraph, "testPrompt.txt");
    String prompt = builder.buildPrompt(diffs);

    assertTrue(prompt.contains("Method: Java:com.example.Foo.bar()"));
    assertTrue(prompt.contains("Calls: Java:com.example.Baz.qux()"));

    assertTrue(prompt.contains("### Java:com.example.Baz.qux()"));
    assertTrue(prompt.contains("void qux() {}"));

    assertFalse(prompt.contains("Method: Java:com.example.Baz.qux()"));
  }

  @Test
  void buildPromptOmitsCallsAndCalledByLinesWhenThereAreNone() throws IOException {
    CallGraph callGraph = new CallGraph();
    JavaMethodSignature changed =
        new JavaMethodSignature("com.example.Lonely.method()", "src/Lonely.java");
    callGraph.registerMethod(changed, "void method() {}");

    Map<String, String> diffs = Map.of("src/Lonely.java", "diff");

    ReviewContextBuilder builder = new ReviewContextBuilder(callGraph, "testPrompt.txt");
    String prompt = builder.buildPrompt(diffs);

    assertTrue(prompt.contains("Method: Java:com.example.Lonely.method()"));
    assertFalse(prompt.contains("Calls:"));
    assertFalse(prompt.contains("Called by:"));
  }

  @Test
  void sourceUnavailablePlaceholderIsUsedWhenCallGraphHasNoSourceForAChangedMethod()
      throws IOException {
    CallGraph callGraph = new CallGraph();
    JavaMethodSignature changed =
        new JavaMethodSignature("com.example.NoSource.method()", "src/NoSource.java");

    // Only linked via addCall, never registered with a source string.
    JavaMethodSignature callee =
        new JavaMethodSignature("com.example.Other.method()", "src/Other.java");
    callGraph.addCall(changed, callee);

    Map<String, String> diffs = Map.of("src/NoSource.java", "diff");

    ReviewContextBuilder builder = new ReviewContextBuilder(callGraph, "testPrompt.txt");
    String prompt = builder.buildPrompt(diffs);

    assertTrue(prompt.contains("(source unavailable"));
  }

  @Test
  void buildPromptThrowsIOExceptionWhenPromptResourceIsMissing() {
    ReviewContextBuilder builder = new ReviewContextBuilder(new CallGraph(), "doesNotExist.txt");

    IOException ex = assertThrows(IOException.class, () -> builder.buildPrompt(Map.of()));
    assertTrue(ex.getMessage().contains("Template not found"));
  }
}
