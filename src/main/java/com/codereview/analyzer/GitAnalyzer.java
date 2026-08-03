package com.codereview.analyzer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

        Map<String, String> diffbyFile = new LinkedHashMap<>();

        try(Git git = new Git(repo); RevWalk walk = new RevWalk((repo))){
            RevCommit baseCommit = walk.parseCommit(repo.resolve(baseRef));

            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(repo.newObjectReader(), baseCommit.getTree());

            FileTreeIterator workingTree = new FileTreeIterator(repo);
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(workingTree)
                    .call();

            for (DiffEntry entry : diffs) {
                String path = repoPath.resolve(entry.getNewPath()).normalize().toString();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(repo);
                    formatter.format(entry);
                }
                diffbyFile.put(path, out.toString());
            }

            return diffbyFile;
        }  catch(AmbiguousObjectException | GitAPIException e) {
            throw new IOException(e);
        }
    }

}
