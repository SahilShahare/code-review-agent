package com.codereview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {

  @TempDir Path repoDir;

  @Test
  void parseArgsThrowsIllegalArgumentExceptionWhenFewerThanTwoArgumentsGiven() {
    assertThrows(IllegalArgumentException.class, () -> invokeParseArgs("only-one-arg"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionForUnknownOption() throws Exception {
    createSrcRoot();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeParseArgs(repoDir.toString(), "src/main/java", "--bogus"));

    assertTrue(ex.getMessage().contains("Unknown option"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionWhenModelFlagIsMissingItsValue() throws Exception {
    createSrcRoot();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeParseArgs(repoDir.toString(), "src/main/java", "-m"));

    assertTrue(ex.getMessage().contains("Missing value for -m"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionForInvalidModelId() throws Exception {
    createSrcRoot();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeParseArgs(repoDir.toString(), "src/main/java", "-m", "not-a-real-model"));

    assertTrue(ex.getMessage().contains("Invalid model id"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionForTooManyPositionalArguments() throws Exception {
    createSrcRoot();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeParseArgs(repoDir.toString(), "src/main/java", "extra-arg"));

    assertTrue(ex.getMessage().contains("Too many positional arguments"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionWhenRepoPathDoesNotExist() {
    Path missingRepo = repoDir.resolve("does-not-exist");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> invokeParseArgs(missingRepo.toString(), "src"));

    assertTrue(ex.getMessage().contains("Repository directory does not exist"));
  }

  @Test
  void parseArgsThrowsIllegalArgumentExceptionWhenSrcRootDoesNotExist() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeParseArgs(repoDir.toString(), "no-such-src"));

    assertTrue(ex.getMessage().contains("Source root does not exist"));
  }

  @Test
  void parseArgsUsesDefaultModelAndBaseRefWhenOptionsAreOmitted() throws Exception {
    createSrcRoot();

    Object config = invokeParseArgs(repoDir.toString(), "src/main/java");

    assertEquals(repoDir.toAbsolutePath().normalize(), accessor(config, "repoPath"));
    assertEquals(
        repoDir.toAbsolutePath().normalize().resolve("src/main/java").normalize(),
        accessor(config, "srcRoot"));
    assertEquals("HEAD", accessor(config, "baseRef"));
    assertEquals("gemini-3.5-flash", accessor(config, "model"));
  }

  @Test
  void parseArgsAppliesExplicitModelAndBaseRefOptions() throws Exception {
    createSrcRoot();

    Object config =
        invokeParseArgs(
            repoDir.toString(), "src/main/java", "-m", "gemini-3.6-flash", "-b", "HEAD~1");

    assertEquals("gemini-3.6-flash", accessor(config, "model"));
    assertEquals("HEAD~1", accessor(config, "baseRef"));
  }

  private Object invokeParseArgs(String... args) throws Exception {
    Method parseArgs = Main.class.getDeclaredMethod("parseArgs", String[].class);
    parseArgs.setAccessible(true);
    try {
      return parseArgs.invoke(null, (Object) args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException re) {
        throw re;
      }
      throw e;
    }
  }

  private Object accessor(Object record, String name) throws Exception {
    Method method = record.getClass().getDeclaredMethod(name);
    method.setAccessible(true);
    return method.invoke(record);
  }

  private void createSrcRoot() throws Exception {
    Path srcRoot = repoDir.resolve("src/main/java");
    Files.createDirectories(srcRoot);
  }
}
