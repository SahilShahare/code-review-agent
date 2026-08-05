package com.codereview.factory;

import com.codereview.model.records.JavaMethodSignature;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaMethodSignatureFactoryTest {

  @TempDir Path sourceRoot;

  @BeforeEach
  void resetParserConfiguration() {
    // Each test picks its own configuration; make sure one test's symbol solver never
    // leaks into the next via StaticJavaParser's shared static config.
    StaticJavaParser.setConfiguration(new ParserConfiguration());
  }

  @Test
  void buildFromMethodDeclarationUsesResolvedQualifiedSignatureWhenResolvable() throws IOException {
    Path file =
        writeSource(
            "Foo.java", "package com.example;\npublic class Foo {\n  public void bar() {}\n}\n");
    CompilationUnit cu = parseWithSymbolResolution(file);
    MethodDeclaration method = cu.findAll(MethodDeclaration.class).get(0);

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(method, file);

    assertEquals("com.example.Foo.bar()", sig.qualifiedName());
    assertEquals(file.toString(), sig.filePath());
  }

  @Test
  void buildFromMethodDeclarationFallsBackToFileAndSignatureWhenUnresolvable() throws IOException {
    // No symbol solver configured (see resetParserConfiguration), so method.resolve() throws
    // and the factory must fall back to the file#signature form instead of propagating.
    Path file = writeSource("Bar.java", "public class Bar {\n  void baz() {}\n}\n");
    CompilationUnit cu = StaticJavaParser.parse(file);
    MethodDeclaration method = cu.findAll(MethodDeclaration.class).get(0);

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(method, file);

    assertTrue(sig.qualifiedName().startsWith(file + "#"));
    assertTrue(sig.qualifiedName().contains("baz"));
    assertEquals(file.toString(), sig.filePath());
  }

  @Test
  void buildFromConstructorDeclarationUsesResolvedQualifiedSignatureWhenResolvable()
      throws IOException {
    Path file =
        writeSource(
            "Widget.java",
            "package com.example;\npublic class Widget {\n  public Widget() {}\n}\n");
    CompilationUnit cu = parseWithSymbolResolution(file);
    ConstructorDeclaration ctor = cu.findAll(ConstructorDeclaration.class).get(0);

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(ctor, file);

    assertEquals("com.example.Widget.Widget()", sig.qualifiedName());
    assertEquals(file.toString(), sig.filePath());
  }

  @Test
  void buildFromConstructorDeclarationFallsBackToFileAndSignatureWhenUnresolvable()
      throws IOException {
    Path file = writeSource("Gadget.java", "public class Gadget {\n  public Gadget(int x) {}\n}\n");
    CompilationUnit cu = StaticJavaParser.parse(file);
    ConstructorDeclaration ctor = cu.findAll(ConstructorDeclaration.class).get(0);

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(ctor, file);

    assertTrue(sig.qualifiedName().startsWith(file + "#"));
    assertEquals(file.toString(), sig.filePath());
  }

  @Test
  void buildFromResolvedMethodDeclarationUsesSourceFilePathWhenAstIsAvailable() throws IOException {
    Path file =
        writeSource(
            "Baz.java", "package com.example;\npublic class Baz {\n  public void qux() {}\n}\n");
    CompilationUnit cu = parseWithSymbolResolution(file);
    MethodDeclaration method = cu.findAll(MethodDeclaration.class).get(0);
    ResolvedMethodDeclaration resolved = method.resolve();

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(resolved);

    assertEquals("com.example.Baz.qux()", sig.qualifiedName());
    assertEquals(file.toString(), sig.filePath());
  }

  @Test
  void buildFromResolvedMethodDeclarationUsesExternalPlaceholderWhenNoSourceIsAvailable()
      throws IOException {
    Path file =
        writeSource(
            "UsesJdkType.java",
            "package com.example;\npublic class UsesJdkType {\n"
                + "  public void run() {\n"
                + "    Object o = new Object();\n"
                + "    o.toString();\n"
                + "  }\n"
                + "}\n");
    CompilationUnit cu = parseWithSymbolResolution(file);
    MethodCallExpr call = cu.findAll(MethodCallExpr.class).get(0);
    ResolvedMethodDeclaration resolved = call.resolve();

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(resolved);

    assertEquals("<external>", sig.filePath());
  }

  @Test
  void buildFromResolvedConstructorDeclarationUsesSourceFilePathWhenAstIsAvailable()
      throws IOException {
    Path file =
        writeSource(
            "Thing.java", "package com.example;\npublic class Thing {\n  public Thing() {}\n}\n");
    CompilationUnit cu = parseWithSymbolResolution(file);
    ConstructorDeclaration ctor = cu.findAll(ConstructorDeclaration.class).get(0);
    ResolvedConstructorDeclaration resolved = ctor.resolve();

    JavaMethodSignature sig = JavaMethodSignatureFactory.build(resolved);

    assertEquals("com.example.Thing.Thing()", sig.qualifiedName());
    assertEquals(file.toString(), sig.filePath());
  }

  private CompilationUnit parseWithSymbolResolution(Path file) throws IOException {
    CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    typeSolver.add(new ReflectionTypeSolver());
    typeSolver.add(new JavaParserTypeSolver(sourceRoot));

    StaticJavaParser.setConfiguration(
        new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver)));

    return StaticJavaParser.parse(file);
  }

  private Path writeSource(String fileName, String source) throws IOException {
    Path file = sourceRoot.resolve(fileName);
    Files.writeString(file, source, StandardCharsets.UTF_8);
    return file;
  }
}
