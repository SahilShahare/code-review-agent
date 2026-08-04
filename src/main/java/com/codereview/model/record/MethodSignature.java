package com.codereview.model.record;

public interface MethodSignature {
  String canonicalId();

  String qualifiedName();

  String filePath();
}
