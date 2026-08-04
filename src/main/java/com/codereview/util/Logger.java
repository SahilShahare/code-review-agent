package com.codereview.util;

public class Logger {

  // Per-call resolution noise (unresolved method calls, etc.) is real but not actionable
  // for most runs -- it's gated behind this flag instead of always printing at [ERROR].
  private static final boolean DEBUG_ENABLED =
      "1".equals(System.getenv("CODE_REVIEW_DEBUG"))
          || "true".equalsIgnoreCase(System.getenv("CODE_REVIEW_DEBUG"));

  private Logger() {}

  public static void info(String message) {
    System.out.println("[INFO] " + message);
  }

  public static void warn(String message) {
    System.err.println("[WARN] " + message);
  }

  public static void error(String message) {
    System.err.println("[ERROR] " + message);
  }

  public static void debug(String message) {
    if (DEBUG_ENABLED) {
      System.err.println("[DEBUG] " + message);
    }
  }
}
