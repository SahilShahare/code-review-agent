package com.codereview;

import com.codereview.analyzer.AstAnalyzer;
import com.codereview.analyzer.GitAnalyzer;
import com.codereview.builder.ReviewContextBuilder;
import com.codereview.client.LLMClient;
import com.codereview.factory.AstAnalyzerFactory;
import com.codereview.factory.LLMClientFactory;
import com.codereview.model.CallGraph;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: java -jar ast-review-agent.jar <repo-path> <src-root> [baseRef]");
      System.err.println("  e.g.: java -jar ast-review-agent.jar . src/main/java HEAD");
      System.exit(1);
    }

    String repoPath = args[0];
    Path srcRoot = Path.of(repoPath, args[1]);
    String headRef = args.length > 3 ? args[3] : "HEAD";

    String model = System.getenv().getOrDefault("LLM_MODEL", "gemini-3.5-flash");

    LLMClient llmClient = LLMClientFactory.build(model);

    Set<AstAnalyzer> analyzers = AstAnalyzerFactory.all();

    System.out.println("Parsing source tree and building call graph...");
    CallGraph callGraph = new CallGraph();
    for (AstAnalyzer analyzer : analyzers) {
      analyzer.analyze(srcRoot, callGraph);
    }
    System.out.println("Indexed " + callGraph.allMethods().size() + " methods.");

    try (Repository repo =
        new FileRepositoryBuilder().setGitDir(new File(repoPath, ".git")).build()) {
      Map<String, String> diffs = new GitAnalyzer(repo).getDiff(headRef);
      if (diffs.isEmpty()) {
        System.out.println("No changed files from previous commit.");
        return;
      }

      ReviewContextBuilder contextBuilder = new ReviewContextBuilder(callGraph, "reviewPrompt.txt");
      System.out.println("Reviewing " + diffs.size() + " changed file(s) in one pass...\n");
      String prompt = contextBuilder.buildPrompt(diffs);
      String review = llmClient.review(prompt);
      System.out.println(review);
    }
  }
}
