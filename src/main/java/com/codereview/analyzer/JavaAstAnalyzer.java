package com.codereview.analyzer;

import com.codereview.constants.Constants;
import com.codereview.factory.JavaMethodSignatureFactory;
import com.codereview.model.CallGraph;
import com.codereview.model.enums.Language;
import com.codereview.model.records.JavaMethodSignature;
import com.codereview.util.Logger;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaAstAnalyzer implements AstAnalyzer {

  private final AtomicInteger unresolvedCount = new AtomicInteger();

  @Override
  public void analyze(Path sourceRoot, CallGraph callGraph) throws IOException {
    CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    typeSolver.add(new ReflectionTypeSolver());
    typeSolver.add(new JavaParserTypeSolver(sourceRoot));

    ParserConfiguration config =
        new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));

    StaticJavaParser.setConfiguration(config);

    for (Path file : collectSourceFiles(sourceRoot)) {
      parseFile(file, callGraph);
    }

    if (unresolvedCount.get() > 0) {
      Logger.info(
          unresolvedCount.get()
              + " call(s)/instantiation(s) could not be resolved and are missing from the call "
              + "graph (usually calls into a dependency not on the classpath). Set "
              + "CODE_REVIEW_DEBUG=1 to see each one.");
    }
  }

  private List<Path> collectSourceFiles(Path sourceRoot) throws IOException {
    List<Path> sourceFiles = new ArrayList<>();

    Files.walkFileTree(
        sourceRoot,
        new SimpleFileVisitor<>() {
          @Override
          @SuppressWarnings("NullableProblems")
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            boolean isExcluded =
                !dir.equals(sourceRoot)
                    && Constants.EXCLUDED_SOURCE_DIRS.contains(dir.getFileName().toString());

            return isExcluded ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
          }

          @Override
          @SuppressWarnings("NullableProblems")
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isJavaSource(file)) {
              sourceFiles.add(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });

    return sourceFiles;
  }

  private boolean isJavaSource(Path file) {
    return Language.JAVA.getExtensions().stream()
        .anyMatch(ext -> file.toString().endsWith(String.format(".%s", ext)));
  }

  private void parseFile(Path file, CallGraph callGraph) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);

      cu.findAll(MethodDeclaration.class)
          .forEach(
              method -> {
                JavaMethodSignature callerSig = JavaMethodSignatureFactory.build(method, file);
                registerCallable(callerSig, method, file, callGraph);
              });

      cu.findAll(ConstructorDeclaration.class)
          .forEach(
              ctor -> {
                JavaMethodSignature callerSig = JavaMethodSignatureFactory.build(ctor, file);
                registerCallable(callerSig, ctor, file, callGraph);
              });
    } catch (IOException e) {
      Logger.error(String.format("Could not read %s: %s", file, e.getMessage()));

    } catch (ParseProblemException e) {
      /* A file with a syntax error (WIP branch, broken template, etc.) shouldn't
      take down analysis of every other file -- skip it and keep going. */
      Logger.error(String.format("Skipping %s (syntax error): %s", file, e.getMessage()));
    }
  }

  private void registerCallable(
      JavaMethodSignature callerSig,
      CallableDeclaration<?> declaration,
      Path file,
      CallGraph callGraph) {
    callGraph.registerMethod(callerSig, declaration.toString());

    declaration
        .findAll(MethodCallExpr.class)
        .forEach(
            call -> {
              try {
                ResolvedMethodDeclaration resolved = call.resolve();
                callGraph.addCall(callerSig, JavaMethodSignatureFactory.build(resolved));
              } catch (Exception unresolved) {
                logSkip(call, file);
              }
            });

    declaration
        .findAll(ObjectCreationExpr.class)
        .forEach(
            creation -> {
              try {
                ResolvedConstructorDeclaration resolved = creation.resolve();
                callGraph.addCall(callerSig, JavaMethodSignatureFactory.build(resolved));
              } catch (Exception unresolved) {
                logSkip(creation, file);
              }
            });

    declaration
        .findAll(ExplicitConstructorInvocationStmt.class)
        .forEach(
            invocation -> {
              try {
                ResolvedConstructorDeclaration resolved = invocation.resolve();
                callGraph.addCall(callerSig, JavaMethodSignatureFactory.build(resolved));
              } catch (Exception unresolved) {
                logSkip(invocation, file);
              }
            });
  }

  private void logSkip(Node call, Path file) {
    unresolvedCount.incrementAndGet();
    Logger.debug(
        String.format(
            "Skipping unresolved call %s in %s: source not available on the "
                + "symbol-solver classpath or unsupported generic resolution edge case.",
            call, file));
  }
}
