package com.codereview.factory;

import com.codereview.model.records.JavaMethodSignature;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.nio.file.Path;

public class JavaMethodSignatureFactory {

  private JavaMethodSignatureFactory() {}

  public static JavaMethodSignature build(MethodDeclaration method, Path file) {
    try {
      return new JavaMethodSignature(method.resolve().getQualifiedSignature(), file.toString());
    } catch (Exception e) {
      return new JavaMethodSignature(
          String.format("%s#%s", file.toString(), method.getSignature().asString()),
          file.toString());
    }
  }

  public static JavaMethodSignature build(ResolvedMethodDeclaration method) {
    String qualifiedName = method.getQualifiedSignature();

    String file =
        method
            .toAst()
            .flatMap(Node::findCompilationUnit)
            .flatMap(CompilationUnit::getStorage)
            .map(storage -> storage.getPath().toString())
            .orElse("<external>");

    return new JavaMethodSignature(qualifiedName, file);
  }

  public static JavaMethodSignature build(ConstructorDeclaration ctor, Path file) {
    try {
      return new JavaMethodSignature(ctor.resolve().getQualifiedSignature(), file.toString());
    } catch (Exception e) {
      return new JavaMethodSignature(
          String.format("%s#%s", file.toString(), ctor.getSignature().asString()), file.toString());
    }
  }

  public static JavaMethodSignature build(ResolvedConstructorDeclaration ctor) {
    String qualifiedName = ctor.getQualifiedSignature();

    String file =
        ctor.toAst()
            .flatMap(Node::findCompilationUnit)
            .flatMap(CompilationUnit::getStorage)
            .map(storage -> storage.getPath().toString())
            .orElse("<external>");

    return new JavaMethodSignature(qualifiedName, file);
  }
}
