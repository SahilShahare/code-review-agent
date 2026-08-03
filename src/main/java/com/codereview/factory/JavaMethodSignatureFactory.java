package com.codereview.factory;

import com.codereview.model.signatures.JavaMethodSignature;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.nio.file.Path;

public class JavaMethodSignatureFactory {
    private JavaMethodSignatureFactory() {}

    public static JavaMethodSignature build(MethodDeclaration method, Path file) {
        try{
            return new JavaMethodSignature(method.resolve().getQualifiedSignature(), file.toString());
        } catch (Exception e) {
            return new JavaMethodSignature(String.format("%s#%s",file.toString(),method.getSignature().asString()), file.toString());
        }
    }

    public static JavaMethodSignature build(ResolvedMethodDeclaration method) {
        String qualifiedName = method.getQualifiedSignature();

        String file = method.toAst()
                .flatMap(decl -> decl.findCompilationUnit())
                .flatMap(cu -> cu.getStorage())
                .map(storage -> storage.getPath().toString())
                .orElse("<external>");

        return new JavaMethodSignature(qualifiedName, file);
    }
}
