package com.codereview;

import com.codereview.analyzer.AstAnalyzer;
import com.codereview.analyzer.GitAnalyzer;
import com.codereview.builder.ReviewContextBuilder;
import com.codereview.client.LLMClient;
import com.codereview.factory.AstAnalyzerFactory;
import com.codereview.factory.LLMClientFactory;
import com.codereview.model.CallGraph;
import com.codereview.model.enums.LLMRegistry;
import com.codereview.model.records.ParseResult;
import com.codereview.model.records.UnparsedBlock;
import com.codereview.util.Logger;
import com.codereview.util.ReviewParser;
import com.codereview.util.ReviewPrinter;
import com.codereview.util.TerminalColors;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codereview.constants.Constants.DEFAULT_BASE_REF;
import static com.codereview.constants.Constants.DEFAULT_MODEL;
import static com.codereview.constants.Constants.PROMPT_FILE;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    long start = System.currentTimeMillis();

    try {
      ReviewConfig config = parseArgs(args);

      Logger.info("Repository : " + config.repoPath());
      Logger.info("Source root: " + config.srcRoot());
      Logger.info("Base ref : " + config.baseRef());
      Logger.info("Model : " + config.model());

      CallGraph callGraph = buildCallGraph(config.srcRoot());

      try (Repository repo = openRepository(config.repoPath())) {

        Map<String, String> diffs = new GitAnalyzer(repo).getDiff(config.baseRef());

        if (diffs.isEmpty()) {
          Logger.info("No changed files found.");
          return;
        }

        review(callGraph, diffs, config.model());
      }

      Logger.info("Completed in " + (System.currentTimeMillis() - start) + " ms");

    } catch (IllegalArgumentException e) {
      Logger.error(e.getMessage());
      printUsage();
      System.exit(1);

    } catch (Exception e) {
      Logger.error("Review failed: " + e.getCause());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static ReviewConfig parseArgs(String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Missing required arguments.");
    }

    String model = DEFAULT_MODEL;
    String baseRef = DEFAULT_BASE_REF;

    int positionalCount = 0;
    String repoArg = null;
    String srcArg = null;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      switch (arg) {
        case "-m", "--model" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for " + arg);
          }
          model = args[++i];
          if (!LLMRegistry.isValidModel(model)) {
            throw new IllegalArgumentException(
                "Invalid model id '" + model + "'. Supported models: " + LLMRegistry.allModels());
          }
        }

        case "-b", "--base-ref" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for " + arg);
          }
          baseRef = args[++i];
        }

        default -> {
          if (arg.startsWith("-")) {
            throw new IllegalArgumentException("Unknown option: " + arg);
          }

          if (positionalCount == 0) {
            repoArg = arg;
          } else if (positionalCount == 1) {
            srcArg = arg;
          } else {
            throw new IllegalArgumentException("Too many positional arguments.");
          }
          positionalCount++;
        }
      }
    }

    if (repoArg == null || srcArg == null) {
      throw new IllegalArgumentException("repo-path and src-root are required.");
    }

    Path repoPath = Path.of(repoArg).toAbsolutePath().normalize();
    Path srcRoot = repoPath.resolve(srcArg).normalize();

    validatePaths(repoPath, srcRoot);

    return new ReviewConfig(repoPath, srcRoot, baseRef, model);
  }

  private static void validatePaths(Path repoPath, Path srcRoot) {
    if (!Files.isDirectory(repoPath)) {
      throw new IllegalArgumentException("Repository directory does not exist: " + repoPath);
    }

    if (!Files.isDirectory(srcRoot)) {
      throw new IllegalArgumentException("Source root does not exist: " + srcRoot);
    }
  }

  private static void printUsage() {
    System.out.println(
        """
            Usage:
                java -jar code-review-agent.jar <repo-path> <src-root> [options]

            Mandatory:
                repo-path         Path to the Git repository
                src-root          Source root relative to repo-path

            Optional:
                -m, --model       LLM model to use
                -b, --base-ref    Git reference to compare against

            Examples:
                java -jar code-review-agent.jar . src/main/java
                java -jar code-review-agent.jar . src/main/java -m gemini-3.5-flash
                java -jar code-review-agent.jar . src/main/java -b HEAD~1
                java -jar code-review-agent.jar . src/main/java -m gemini-3.6-flash -b HEAD
        """);
  }

  private static CallGraph buildCallGraph(Path srcRoot) throws Exception {
    Logger.info("Building call graph...");

    Set<AstAnalyzer> analyzers = AstAnalyzerFactory.all();
    CallGraph callGraph = new CallGraph();

    for (AstAnalyzer analyzer : analyzers) {
      Logger.info("Running analyzer: " + analyzer.getClass().getSimpleName());

      analyzer.analyze(srcRoot, callGraph);
    }

    return callGraph;
  }

  private static Repository openRepository(Path repoPath) throws Exception {
    FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(repoPath.toFile());

    if (builder.getGitDir() == null) {
      throw new IllegalArgumentException("No Git repository found at: " + repoPath);
    }

    return builder.build();
  }

  private static void review(CallGraph callGraph, Map<String, String> diffs, String model)
      throws Exception {
    Logger.info("Reviewing " + diffs.size() + " changed file(s)...");

    ReviewContextBuilder contextBuilder = new ReviewContextBuilder(callGraph, PROMPT_FILE);

    String prompt = contextBuilder.buildPrompt(diffs);

    LLMClient client = LLMClientFactory.build(model);

    String response = client.review(prompt);

    ParseResult result = ReviewParser.parse(response);

    if (!result.findings().isEmpty()) {
      ReviewPrinter.print(result.findings(), System.out);
    }

    if (!result.unparsed().isEmpty()) {
      // These blocks failed the FILE/SEVERITY/MESSAGE contract (already logged by
      // ReviewParser) -- show them as plain, colorized text instead of dropping the
      // findings they might still contain.
      String rawJoined =
          result.unparsed().stream()
              .map(UnparsedBlock::rawText)
              .collect(Collectors.joining("\n\n---\n\n"));

      System.out.println("\n" + TerminalColors.colorizeReview(rawJoined));
    }

    if (result.findings().isEmpty() && result.unparsed().isEmpty()) {
      Logger.info("No findings.");
    }
  }

  private record ReviewConfig(Path repoPath, Path srcRoot, String baseRef, String model) {}
}
