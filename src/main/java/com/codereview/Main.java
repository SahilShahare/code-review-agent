package com.codereview;

import com.codereview.analyzer.AstAnalyzer;
import com.codereview.analyzer.GitAnalyzer;
import com.codereview.analyzer.JavaAstAnalyzer;
import com.codereview.builder.ReviewContextBuilder;
import com.codereview.model.CallGraph;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

//        String apiKey = System.getenv("GEMINI_API_KEY");
//        if (apiKey == null || apiKey.isBlank()) {
//            System.err.println("Set GEMINI_API_KEY in your environment.");
//            System.exit(1);
//        }

        String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-pro");

        List<AstAnalyzer> analyzers = List.of(
                new JavaAstAnalyzer()
        );

        System.out.println("Parsing source tree and building call graph...");
        CallGraph callGraph = new CallGraph();
        for (AstAnalyzer analyzer : analyzers) {
            analyzer.analyze(srcRoot, callGraph);
        }
        System.out.println("Indexed " + callGraph.allMethods().size() + " methods.");

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(new File(repoPath, ".git"))
                .build()) {
            Map<String, String> diffs = new GitAnalyzer(repo).getDiff(headRef);
            if (diffs.isEmpty()) {
                System.out.println("No changed files from previous commit.");
                return;
            }

            // 3. Build one prompt covering every changed file and send it in a single
            // call -- this lets the model reason about interactions across files,
            // not just within each file in isolation. Files outside supportedExtensions
            // still get included, just without a structural-context section, since
            // there's no call graph data for them.
            ReviewContextBuilder contextBuilder = new ReviewContextBuilder(callGraph, "reviewPrompt.txt");
            System.out.println("Reviewing " + diffs.size() + " changed file(s) in one pass...\n");
            String prompt = contextBuilder.buildPrompt(diffs);
            System.out.println(prompt);
        }


    }
}