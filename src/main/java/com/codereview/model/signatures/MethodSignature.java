package com.codereview.model.signatures;

import com.codereview.model.enums.Language;

public interface MethodSignature {
    public String canonicalId();
    public String qualifiedName();
    public Language language();
    public String filePath();
}
