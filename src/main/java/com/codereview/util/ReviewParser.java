package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.record.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the LLM's response into structured Finding objects, per the format requested in
 * review-prompt's template (FILE/SEVERITY/LOCATION/MESSAGE blocks separated by "---" lines).
 * Deliberately tolerant: a model that doesn't follow the format exactly (extra prose, wrong
 * severity casing, missing field) just yields fewer or zero findings rather than throwing -- Main
 * falls back to plain text output when parsing comes up empty.
 */
public final class ReviewParser {

  private static final Pattern FIELD =
      Pattern.compile("^(FILE|SEVERITY|LOCATION|MESSAGE):\\s?(.*)$");
  private static final Pattern BLOCK_SEPARATOR = Pattern.compile("(?m)^-{3,}\\s*$");

  private ReviewParser() {}

  public static List<Finding> parse(String raw) {
    List<Finding> findings = new ArrayList<>();

    for (String block : BLOCK_SEPARATOR.split(raw)) {
      Finding finding = parseBlock(block);
      if (finding != null) {
        findings.add(finding);
      }
    }

    return findings;
  }

  private static Finding parseBlock(String block) {
    String file = null;
    String location = null;
    Severity severity = null;
    StringBuilder message = new StringBuilder();
    boolean inMessage = false;

    for (String line : block.split("\n")) {
      Matcher m = FIELD.matcher(line.trim());
      if (m.matches()) {
        String key = m.group(1);
        String value = m.group(2).trim();
        inMessage = "MESSAGE".equals(key);
        switch (key) {
          case "FILE" -> file = value;
          case "LOCATION" -> location = value;
          case "SEVERITY" -> severity = parseSeverity(value);
          case "MESSAGE" -> message.append(value);
        }
      } else if (inMessage && !line.isBlank()) {
        // MESSAGE can wrap across lines -- keep appending until the next field.
        message.append(" ").append(line.trim());
      }
    }

    if (file == null || severity == null || message.isEmpty()) {
      return null;
    }
    return new Finding(file, severity, location == null ? "" : location, message.toString().trim());
  }

  private static Severity parseSeverity(String value) {
    try {
      return Severity.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
