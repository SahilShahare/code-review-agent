package com.codereview.model.records;

public interface MethodSignature {
  String canonicalId();

  String qualifiedName();

  String filePath();
}
