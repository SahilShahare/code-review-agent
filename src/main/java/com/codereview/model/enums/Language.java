package com.codereview.model.enums;

import java.util.Set;

public enum Language {
  JAVA("Java", Set.of("java"));

  private final String name;
  private final Set<String> extensions;

  Language(String name, Set<String> extensions) {
    this.name = name;
    this.extensions = extensions;
  }

  public String getName() {
    return this.name;
  }

  public Set<String> getExtensions() {
    return this.extensions;
  }
}
