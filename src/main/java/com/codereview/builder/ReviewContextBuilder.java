package com.codereview.builder;

import com.codereview.model.CallGraph;
import com.codereview.model.records.MethodSignature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codereview.constants.Constants.PROMPT_DIRECTORY;
import static com.codereview.constants.Constants.SOURCE_UNAVAILABLE;

public class ReviewContextBuilder {

  private static final String COMBINED_SECTION_TEMPLATE =
      """
                --- CHANGED FILES (DIFFS) ---
                %s
                --- CALL GRAPH (changed methods, their callees, their callers) ---
                %s""";
  private static final String METHOD = "Method: ";
  private static final String CALLS = "  Calls: ";
  private static final String CALLED_BY = "  Called by: ";
  private static final String NEW_LINE = "\n";
  private static final String COMMA = ", ";
  private static final String HASH = "### ";
  private static final String FILE = "=== File: ";
  private static final String FILE_END = " ===";

  private final CallGraph callGraph;
  private final String promptFile;

  public ReviewContextBuilder(CallGraph callGraph, String promptFile) {
    this.callGraph = callGraph;
    this.promptFile = promptFile;
  }

  public String buildPrompt(Map<String, String> diffsByFile) throws IOException {
    Map<String, String> sourceByCanonicalId = new LinkedHashMap<>();

    StringBuilder diffSection = new StringBuilder();

    for (var entry : diffsByFile.entrySet()) {
      diffSection.append(FILE).append(entry.getKey()).append(FILE_END).append(NEW_LINE);
      diffSection.append(entry.getValue()).append(NEW_LINE).append(NEW_LINE);
    }

    Set<MethodSignature> changedMethods =
        callGraph.allMethods().stream()
            .filter(sig -> diffsByFile.containsKey(sig.filePath()))
            .collect(Collectors.toUnmodifiableSet());

    StringBuilder graphSection = new StringBuilder();

    for (MethodSignature changed : changedMethods) {
      Set<MethodSignature> callees = callGraph.getCallees(changed);
      Set<MethodSignature> callers = callGraph.getCallers(changed);

      graphSection.append(METHOD).append(changed.canonicalId()).append(NEW_LINE);

      if (!callees.isEmpty()) {
        graphSection.append(CALLS).append(idsToString(callees)).append(NEW_LINE);
      }

      if (!callers.isEmpty()) {
        graphSection.append(CALLED_BY).append(idsToString(callers)).append(NEW_LINE);
      }

      graphSection.append(NEW_LINE);

      registerSource(changed, sourceByCanonicalId);

      callees.forEach(c -> registerSource(c, sourceByCanonicalId));
      callers.forEach(c -> registerSource(c, sourceByCanonicalId));
    }

    String combinedSection = COMBINED_SECTION_TEMPLATE.formatted(diffSection, graphSection);

    StringBuilder sourceAppendix = new StringBuilder();

    sourceByCanonicalId.forEach(
        (id, source) ->
            sourceAppendix
                .append(HASH)
                .append(id)
                .append(NEW_LINE)
                .append(source)
                .append(NEW_LINE)
                .append(NEW_LINE));

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
    return sigs.stream().map(MethodSignature::canonicalId).collect(Collectors.joining(COMMA));
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
