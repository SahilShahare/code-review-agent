package com.codereview.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoggerTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void redirectStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void infoPrintsToStandardOutWithInfoPrefix() {
        Logger.info("build started");

        assertEquals("[INFO] build started" + System.lineSeparator(), outContent.toString(StandardCharsets.UTF_8));
        assertEquals("", errContent.toString(StandardCharsets.UTF_8));
    }

    @Test
    void warnPrintsToStandardErrWithWarnPrefix() {
        Logger.warn("deprecated flag used");

        assertEquals("[WARN] deprecated flag used" + System.lineSeparator(), errContent.toString(StandardCharsets.UTF_8));
        assertEquals("", outContent.toString(StandardCharsets.UTF_8));
    }

    @Test
    void errorPrintsToStandardErrWithErrorPrefix() {
        Logger.error("failed to parse file");

        assertEquals(
                "[ERROR] failed to parse file" + System.lineSeparator(), errContent.toString(StandardCharsets.UTF_8));
        assertEquals("", outContent.toString(StandardCharsets.UTF_8));
    }

    @Test
    void debugPrintsNothingWhenDebugFlagIsNotSetInTheEnvironment() {
        // DEBUG_ENABLED is resolved once from CODE_REVIEW_DEBUG when the class is loaded, so this
        // assertion only holds in an environment where the flag isn't set. If it is, skip rather
        // than fail -- the flag is genuinely on and Logger.debug is expected to print.
        Assumptions.assumeTrue(
                System.getenv("CODE_REVIEW_DEBUG") == null,
                "CODE_REVIEW_DEBUG is set in this environment; skipping disabled-debug test");

        Logger.debug("resolving symbol X");

        assertEquals("", errContent.toString(StandardCharsets.UTF_8));
        assertEquals("", outContent.toString(StandardCharsets.UTF_8));
    }
}