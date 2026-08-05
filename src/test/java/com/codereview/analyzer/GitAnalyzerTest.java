package com.codereview.analyzer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitAnalyzerTest {
  @TempDir Path repoDir;

  private Git git;
  private Repository repository;
  private Path trackedFile;

  @BeforeEach
  void setUp() throws Exception {
    git = Git.init().setDirectory(repoDir.toFile()).call();
    repository = git.getRepository();

    trackedFile = repoDir.resolve("Foo.java");
    Files.writeString(trackedFile, "class Foo {\n}\n", StandardCharsets.UTF_8);

    git.add().addFilepattern(".").call();
    git.commit()
        .setMessage("initial commit")
        .setAuthor("Test", "test@example.com")
        .setCommitter("Test", "test@example.com")
        .call();
  }

  @AfterEach
  void tearDown() {
    git.close();
    repository.close();
  }

  @Test
  void getDiffReturnsNoEntriesWhenWorkingTreeMatchesBaseRef() throws IOException {
    GitAnalyzer analyzer = new GitAnalyzer(repository);

    Map<String, String> diffs = analyzer.getDiff("HEAD");

    assertTrue(diffs.isEmpty());
  }

  @Test
  void getDiffDetectsUncommittedModificationToTrackedFile() throws IOException {
    Files.writeString(trackedFile, "class Foo {\n  void bar() {}\n}\n", StandardCharsets.UTF_8);

    GitAnalyzer analyzer = new GitAnalyzer(repository);
    Map<String, String> diffs = analyzer.getDiff("HEAD");

    String expectedKey = repoDir.resolve("Foo.java").toAbsolutePath().normalize().toString();

    assertTrue(diffs.containsKey(expectedKey), "Expected diff map to contain key: " + expectedKey);
    assertTrue(diffs.get(expectedKey).contains("bar"));
  }

  @Test
  void getDiffDetectsNewUntrackedButAddedFile() throws Exception {
    Path newFile = repoDir.resolve("Bar.java");
    Files.writeString(newFile, "class Bar {}\n", StandardCharsets.UTF_8);
    git.add().addFilepattern("Bar.java").call();

    GitAnalyzer analyzer = new GitAnalyzer(repository);
    Map<String, String> diffs = analyzer.getDiff("HEAD");

    String expectedKey = repoDir.resolve("Bar.java").toAbsolutePath().normalize().toString();
    assertTrue(diffs.containsKey(expectedKey));
  }

  @Test
  void getDiffDetectsDeletedTrackedFile() throws Exception {
    Files.delete(trackedFile);

    GitAnalyzer analyzer = new GitAnalyzer(repository);
    Map<String, String> diffs = analyzer.getDiff("HEAD");

    String expectedKey = repoDir.resolve("Foo.java").toAbsolutePath().normalize().toString();

    assertTrue(diffs.containsKey(expectedKey), "Expected diff map to contain key: " + expectedKey);
    assertTrue(diffs.get(expectedKey).contains("-class Foo"));
  }

  @Test
  void getDiffThrowsIllegalArgumentExceptionForUnresolvableRef() {
    GitAnalyzer analyzer = new GitAnalyzer(repository);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.getDiff("this-branch-definitely-does-not-exist"));

    assertTrue(ex.getMessage().contains("Unknown ref"));
  }
}
