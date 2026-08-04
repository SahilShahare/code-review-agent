package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.records.Finding;
import com.codereview.model.records.ParseResult;
import com.codereview.model.records.UnparsedBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReviewParser {

  private static final Pattern FIELD =
      Pattern.compile("^(FILE|SEVERITY|LOCATION|MESSAGE):\\s?(.*)$");
  private static final Pattern BLOCK_SEPARATOR = Pattern.compile("(?m)^-{3,}\\s*$");

  private ReviewParser() {}

  public static ParseResult parse(String raw) {
    List<Finding> findings = new ArrayList<>();
    List<UnparsedBlock> unparsed = new ArrayList<>();

    for (String block : BLOCK_SEPARATOR.split(raw)) {
      if (block.isBlank()) {
        // Empty segments show up around leading/trailing separators -- not a format drift,
        // just an artifact of splitting on "---".
        continue;
      }

      Finding finding = parseBlock(block);

      if (finding != null) {
        findings.add(finding);
      } else {
        String trimmed = block.trim();
        Logger.error(
            "Could not parse a review block into FILE/SEVERITY/MESSAGE -- the model may have "
                + "drifted from the expected format. Raw block:\n"
                + trimmed);
        unparsed.add(new UnparsedBlock(trimmed));
      }
    }

    return new ParseResult(findings, unparsed);
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
