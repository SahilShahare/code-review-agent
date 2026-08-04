package com.codereview.factory;

import com.codereview.analyzer.AstAnalyzer;
import com.codereview.analyzer.JavaAstAnalyzer;
import com.codereview.model.enums.Language;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AstAnalyzerFactory {
    private AstAnalyzerFactory() {}

    public static Optional<AstAnalyzer> build(Language lang) {
        switch(lang) {
            case JAVA :
                return Optional.of(new JavaAstAnalyzer());
            default:
                return Optional.empty();
        }
    }

    public static Set<AstAnalyzer> all() {
        return Arrays.stream(Language.values())
                .map(AstAnalyzerFactory::build)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }
}
