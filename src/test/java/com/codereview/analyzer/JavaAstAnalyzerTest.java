package com.codereview.analyzer;

import com.codereview.model.CallGraph;
import com.codereview.model.records.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaAstAnalyzerTest {

  @TempDir Path sourceRoot;

  @Test
  void analyzeRegistersMethodAndConstructorDeclarationsWithResolvedCallEdges() throws Exception {
    writeSource(
        "com/example/Baz.java",
        "package com.example;\n"
            + "public class Baz {\n"
            + "  public Baz() {}\n"
            + "  public void qux() {}\n"
            + "}\n");
    writeSource(
        "com/example/Foo.java",
        "package com.example;\n"
            + "public class Foo {\n"
            + "  public void bar() {\n"
            + "    Baz baz = new Baz();\n"
            + "    baz.qux();\n"
            + "  }\n"
            + "}\n");

    CallGraph callGraph = new CallGraph();
    new JavaAstAnalyzer().analyze(sourceRoot, callGraph);

    Set<MethodSignature> methods = callGraph.allMethods();
    assertTrue(methods.stream().anyMatch(m -> m.canonicalId().contains("Foo.bar")));
    assertTrue(methods.stream().anyMatch(m -> m.canonicalId().contains("Baz.qux")));

    MethodSignature fooBar = findByCanonicalIdFragment(callGraph, "Foo.bar");
    Set<MethodSignature> callees = callGraph.getCallees(fooBar);

    assertEquals(2, callees.size());
    assertTrue(callees.stream().anyMatch(c -> c.canonicalId().contains("Baz.qux")));
    assertTrue(callees.stream().anyMatch(c -> c.canonicalId().contains("Baz.Baz")));

    assertTrue(callGraph.getSource(fooBar).contains("baz.qux()"));
  }

  @Test
  void analyzeSkipsFileWithSyntaxErrorButContinuesWithOtherFiles() throws Exception {
    writeSource(
        "com/example/Broken.java",
        "package com.example;\npublic class Broken {\n  public void broken( {\n");
    writeSource(
        "com/example/Good.java",
        "package com.example;\npublic class Good {\n  public void fine() {}\n}\n");

    CallGraph callGraph = new CallGraph();

    assertDoesNotThrow(() -> new JavaAstAnalyzer().analyze(sourceRoot, callGraph));

    assertTrue(
        callGraph.allMethods().stream().anyMatch(m -> m.canonicalId().contains("Good.fine")));
  }

  @Test
  void analyzeSkipsFilesInExcludedSourceDirectories() throws Exception {
    writeSource(
        "target/generated/com/example/Excluded.java",
        "package com.example;\npublic class Excluded {\n  public void excludedMarkerMethod() {}\n}\n");
    writeSource(
        "com/example/Included.java",
        "package com.example;\npublic class Included {\n  public void includedMarkerMethod() {}\n}\n");

    CallGraph callGraph = new CallGraph();
    new JavaAstAnalyzer().analyze(sourceRoot, callGraph);

    assertFalse(
        callGraph.allMethods().stream()
            .anyMatch(m -> m.canonicalId().contains("excludedMarkerMethod")));
    assertTrue(
        callGraph.allMethods().stream()
            .anyMatch(m -> m.canonicalId().contains("includedMarkerMethod")));
  }

  @Test
  void analyzeSkipsUnresolvableCallsWithoutThrowingOrAddingThemToCallGraph() throws Exception {
    writeSource(
        "com/example/Caller.java",
        "package com.example;\n"
            + "public class Caller {\n"
            + "  public void invoke() {\n"
            + "    UnknownDependency dep = new UnknownDependency();\n"
            + "    dep.doSomething();\n"
            + "  }\n"
            + "}\n");

    CallGraph callGraph = new CallGraph();

    assertDoesNotThrow(() -> new JavaAstAnalyzer().analyze(sourceRoot, callGraph));

    MethodSignature invoke = findByCanonicalIdFragment(callGraph, "Caller.invoke");
    assertTrue(callGraph.getCallees(invoke).isEmpty());
  }

  private void writeSource(String relativePath, String content) throws IOException {
    Path file = sourceRoot.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  private MethodSignature findByCanonicalIdFragment(CallGraph callGraph, String fragment) {
    return callGraph.allMethods().stream()
        .filter(sig -> sig.canonicalId().contains(fragment))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No registered method matching: " + fragment));
  }
}
