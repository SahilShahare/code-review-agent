package com.codereview.model.signatures;

import com.codereview.model.enums.Language;

public record JavaMethodSignature(String qualifiedName, String filePath) implements MethodSignature{

    @Override
    public String canonicalId() {
        return String.format("%s:%s", Language.JAVA.getName(), qualifiedName);
    }

    @Override
    public Language language() {
        return Language.JAVA;
    }
}
