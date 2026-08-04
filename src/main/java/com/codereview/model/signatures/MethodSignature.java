package com.codereview.model.signatures;

import com.codereview.model.enums.Language;

public interface MethodSignature {
    String canonicalId();
    String qualifiedName();
    String filePath();
}
