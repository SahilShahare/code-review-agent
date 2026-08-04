package com.codereview.constants;

import java.util.Set;

public final class Constants {

  // Git Constants
  public static final String DEFAULT_BASE_REF = "HEAD";

  // LLM API Constants
  public static final String DEFAULT_MODEL = "gemini-3.5-flash";
  public static final int LLM_API_CONNECTION_TIMEOUT = 60;
  public static final int LLM_API_REQUEST_TIMEOUT = 120;
  public static final String GEMINI_ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

  // PROMPT
  public static final String PROMPT_DIRECTORY = "prompts/";
  public static final String SOURCE_UNAVAILABLE =
      "(source unavailable -- declared outside the parsed source tree)";
  public static final String PROMPT_FILE = "reviewPrompt.txt";
  
  //TEXT STYLE & COLORS
  public static final String RESET = "\u001B[0m";
  public static final String BOLD = "\u001B[1m";
  public static final String RED = "\u001B[31m";
  public static final String YELLOW = "\u001B[33m";
  public static final String CYAN = "\u001B[36m";
  public static final String GREEN = "\u001B[32m";
  public static final String FG_GREEN = "\u001B[32m";
  public static final String FG_WHITE = "\u001B[97m";
  public static final String FG_BLACK = "\u001B[30m";
  public static final String BG_RED = "\u001B[41m";
  public static final String BG_YELLOW = "\u001B[43m";
  public static final String BG_CYAN = "\u001B[46m";
  public static final String FG_BRIGHT_RED = "\u001B[91m"; // <exception>
  public static final String FG_MAGENTA = "\u001B[35m"; // <class>
  public static final String FG_BRIGHT_BLUE = "\u001B[94m"; // <method>

  // AST analysis: directory names to never descend into while scanning for source files.
  public static final Set<String> EXCLUDED_SOURCE_DIRS =
          Set.of(
                  "target",
                  "build",
                  "out",
                  "bin",
                  "node_modules",
                  ".git",
                  ".idea",
                  ".gradle",
                  "generated-sources",
                  "generated-test-sources");

  public Constants() {}
}
