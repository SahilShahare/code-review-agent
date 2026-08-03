package com.codereview.model.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static Set<String> getAllExtensions() {
         return Arrays.stream(values())
                 .flatMap(lang -> lang.extensions.stream())
                 .collect(Collectors.toUnmodifiableSet());
    }
}
