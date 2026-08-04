package com.codereview.builder;

import com.codereview.model.CallGraph;
import com.codereview.model.signatures.MethodSignature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReviewContextBuilder {
  private final CallGraph callGraph;
  private final String promptFile;
  private static final String SOURCE_UNAVAILABLE =
      "(source unavailable -- declared outside the parsed source tree)";
  private static final String PROMPT_DIRECTORY = "prompts/";
  private static final String COMBINED_SECTION_TEMPLATE =
      """
                --- CHANGED FILES (DIFFS) ---
                %s
                --- CALL GRAPH (changed methods, their callees, their callers) ---
                %s""";

  public ReviewContextBuilder(CallGraph callGraph, String promptFile) {
    this.callGraph = callGraph;
    this.promptFile = promptFile;
  }

  public String buildPrompt(Map<String, String> diffsByFile) throws IOException {
    Map<String, String> sourceByCanonicalId = new LinkedHashMap<>();

    StringBuilder diffSection = new StringBuilder();

    for (var entry : diffsByFile.entrySet()) {
      diffSection.append("=== File: ").append(entry.getKey()).append(" ===\n");
      diffSection.append(entry.getValue()).append("\n\n");
    }

    Set<MethodSignature> changedMethods =
        callGraph.allMethods().stream()
            .filter(sig -> diffsByFile.containsKey(sig.filePath()))
            .collect(Collectors.toUnmodifiableSet());

    StringBuilder graphSection = new StringBuilder();

    for (MethodSignature changed : changedMethods) {
      Set<MethodSignature> callees = callGraph.getCallees(changed);
      Set<MethodSignature> callers = callGraph.getCallers(changed);

      graphSection.append("Method: ").append(changed.canonicalId()).append("\n");
      if (!callees.isEmpty()) {
        graphSection.append("  Calls: ").append(idsToString(callees)).append("\n");
      }
      if (!callers.isEmpty()) {
        graphSection.append("  Called by: ").append(idsToString(callers)).append("\n");
      }
      graphSection.append("\n");

      registerSource(changed, sourceByCanonicalId);
      callees.forEach(c -> registerSource(c, sourceByCanonicalId));
      callers.forEach(c -> registerSource(c, sourceByCanonicalId));
    }

    String combinedSection = COMBINED_SECTION_TEMPLATE.formatted(diffSection, graphSection);

    StringBuilder sourceAppendix = new StringBuilder();
    sourceByCanonicalId.forEach(
        (id, source) ->
            sourceAppendix.append("### ").append(id).append("\n").append(source).append("\n\n"));
    return loadPrompt().formatted(combinedSection, sourceAppendix);
  }

  private String loadPrompt() throws IOException {
    String promptPath = PROMPT_DIRECTORY + this.promptFile;

    try (InputStream input = getClass().getClassLoader().getResourceAsStream(promptPath)) {
      if (input == null) {
        throw new IOException("Template not found: " + promptPath);
      }

      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String idsToString(Set<MethodSignature> sigs) {
    return sigs.stream().map(MethodSignature::canonicalId).collect(Collectors.joining(", "));
  }

  private void registerSource(MethodSignature sig, Map<String, String> sourceByCanonicalId) {
    sourceByCanonicalId.computeIfAbsent(
        sig.canonicalId(),
        id -> {
          String source = callGraph.getSource(sig);
          return source != null ? source : SOURCE_UNAVAILABLE;
        });
  }
}
