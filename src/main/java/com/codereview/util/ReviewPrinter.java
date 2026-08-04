package com.codereview.util;

import com.codereview.model.enums.Severity;
import com.codereview.model.record.Finding;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders structured Findings to the terminal with colored severity badges
 * (background color, not just colored text -- reads far better at a glance
 * than a keyword highlighted mid-sentence) grouped under each file.
 */
public final class ReviewPrinter {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String FG_GREEN = "\u001B[32m";
    private static final String FG_WHITE = "\u001B[97m";
    private static final String FG_BLACK = "\u001B[30m";
    private static final String BG_RED = "\u001B[41m";
    private static final String BG_YELLOW = "\u001B[43m";
    private static final String BG_CYAN = "\u001B[46m";

    private ReviewPrinter() {
    }

    public static void print(List<Finding> findings, PrintStream out) {
        Map<String, List<Finding>> byFile = findings.stream()
                .collect(Collectors.groupingBy(Finding::file, LinkedHashMap::new, Collectors.toList()));

        for (var entry : byFile.entrySet()) {
            String file = entry.getKey();
            out.println();
            out.println(BOLD + FG_GREEN + "── " + file + " " + "─".repeat(Math.max(4, 70 - file.length())) + RESET);

            for (Finding f : entry.getValue()) {
                out.println(badge(f.severity()) + " " + BOLD + f.location() + RESET);
                out.println("   " + MessageHighlighter.highlight(f.message()));
                out.println();
            }
        }
    }

    private static String badge(Severity severity) {
        return switch (severity) {
            case BLOCKER -> BG_RED + FG_WHITE + BOLD + " BLOCKER " + RESET;
            case WARNING -> BG_YELLOW + FG_BLACK + BOLD + " WARNING " + RESET;
            case NIT -> BG_CYAN + FG_BLACK + BOLD + "   NIT   " + RESET;
        };
    }
}