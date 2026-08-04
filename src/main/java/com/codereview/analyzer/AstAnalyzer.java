package com.codereview.analyzer;

import com.codereview.model.CallGraph;

import java.io.IOException;
import java.nio.file.Path;

public interface AstAnalyzer {

  void analyze(Path sourceRoot, CallGraph callGraph) throws IOException;
}
