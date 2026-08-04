package com.codereview.analyzer;

import com.codereview.factory.JavaMethodSignatureFactory;
import com.codereview.model.CallGraph;
import com.codereview.model.enums.Language;
import com.codereview.model.signatures.JavaMethodSignature;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public class JavaAstAnalyzer implements AstAnalyzer {

    @Override
    public void analyze(Path sourceRoot, CallGraph callGraph) throws IOException {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(sourceRoot));

        ParserConfiguration config = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);

        try(Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(file -> Language.JAVA.getExtensions()
                    .stream()
                    .anyMatch(ext -> file.toString().endsWith(String.format(".%s", ext))))
                    .forEach(file -> parseFile(file, callGraph));
        }
    }

    private void parseFile(Path file, CallGraph callGraph) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                JavaMethodSignature callerSig = JavaMethodSignatureFactory.build(method, file);
                callGraph.registerMethod(callerSig, method.toString());

                method.findAll(MethodCallExpr.class).forEach(call ->{
                    try {
                        ResolvedMethodDeclaration resolved = call.resolve();
                        callGraph.addCall(callerSig, JavaMethodSignatureFactory.build(resolved));
                    } catch (Exception unresolved) {
                        System.err.println(String.format( "Skipping unresolved method call '%s' in %s: source not " +
                                "available on the symbol-solver classpath or unsupported generic resolution edge case."
                                , call, file));
                    }
                });
            });
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": "+ e.getMessage());
        } catch (ParseProblemException e) {
            /* A file with a syntax error (WIP branch, broken template, etc.) shouldn't
             take down analysis of every other file -- skip it and keep going. */
            System.err.println("Skipping " + file + " (syntax error): " + e.getMessage());
        }
    }
}
