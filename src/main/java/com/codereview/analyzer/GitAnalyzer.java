package com.codereview.analyzer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GitAnalyzer {
  private final Repository repo;
  private final Path repoPath;

  public GitAnalyzer(Repository repo) {
    this.repo = repo;
    this.repoPath = repo.getWorkTree().toPath().toAbsolutePath().normalize();
  }

  public Map<String, String> getDiff(String baseRef) throws IOException {

    Map<String, String> diffByFile = new LinkedHashMap<>();

    try (Git git = new Git(repo);
        RevWalk walk = new RevWalk((repo))) {
      ObjectId baseId = repo.resolve(baseRef);

      if (baseId == null) {
        throw new IllegalArgumentException(
            "Unknown ref: '" + baseRef + "' -- no matching branch, tag, or commit in this repo.");
      }

      RevCommit baseCommit = walk.parseCommit(baseId);

      CanonicalTreeParser oldTree = new CanonicalTreeParser();
      oldTree.reset(repo.newObjectReader(), baseCommit.getTree());

      FileTreeIterator workingTree = new FileTreeIterator(repo);

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      try (DiffFormatter formatter = new DiffFormatter(out)) {
        formatter.setRepository(repo);

        List<DiffEntry> diffs = formatter.scan(oldTree, workingTree);

        for (DiffEntry entry : diffs) {
          out.reset();

          String relativePath =
              entry.getChangeType() == DiffEntry.ChangeType.DELETE
                  ? entry.getOldPath()
                  : entry.getNewPath();

          String path = repoPath.resolve(relativePath).normalize().toString();

          FileHeader header = formatter.toFileHeader(entry);

          if (header.getPatchType() != FileHeader.PatchType.UNIFIED) {
            out.write("(binary file changed)".getBytes(StandardCharsets.UTF_8));
          } else {
            formatter.format(entry);
          }

          diffByFile.put(path, out.toString(StandardCharsets.UTF_8));
        }
      }

      System.out.println(diffByFile);

      return diffByFile;

    } catch (AmbiguousObjectException e) {
      throw new IOException(e);
    }
  }
}
