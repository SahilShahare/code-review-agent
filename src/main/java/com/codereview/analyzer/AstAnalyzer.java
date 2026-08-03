package com.codereview.analyzer;

import com.codereview.model.CallGraph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface AstAnalyzer {

    public Set<String> supportedExtensions();

    public void analyze(Path sourceRoot, CallGraph callGraph) throws IOException;
}
