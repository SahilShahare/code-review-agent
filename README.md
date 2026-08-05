# Code Review Agent

AI-assisted Java code review tool that parses changed source with JavaParser, builds a method-level call graph across the codebase, and sends the changed methods plus their callers/callees to Gemini for contextual review comments.

## Features

- Parses Java source into an AST (Abstract Syntax Tree) using JavaParser with symbol resolution
- Resolves method calls, object creations, and constructor invocations across files
- Builds a method-level call graph (callers and callees) for the whole source root
- Diffs a base Git ref against the current working tree using JGit, scoped to changed files
- Cross-references changed methods against the call graph to include relevant callers/callees as context
- Sends a single combined prompt (diffs + call graph + source) to the Gemini API
- Parses the model's response into structured findings (`BLOCKER` / `WARNING` / `NIT`) and prints them to the terminal with ANSI highlighting
- Falls back to printing raw, unparsed model output if a response block doesn't match the expected format
- Produces JaCoCo line/branch coverage reports

## Tech Stack

| Category            | Technology                                                        |
|----------------------|----------------------------------------------------------------------|
| Language / Runtime    | Java 17                                                              |
| Build Tool             | Maven (`maven-shade-plugin` for the runnable fat jar)                 |
| AST Parsing            | JavaParser (`javaparser-symbol-solver-core`)                          |
| Git Diffing             | JGit (`org.eclipse.jgit`)                                            |
| JSON Handling           | Jackson (`jackson-databind`)                                          |
| AI Provider             | Gemini API (via `java.net.http.HttpClient`)                           |
| Testing                 | JUnit 5, JUnit Pioneer, Mockito                                       |
| Coverage                | JaCoCo (`jacoco-maven-plugin`)                                        |

## Architecture

```
                 ┌───────────────────────┐
                 │   GitAnalyzer         │
                 │   (JGit: base-ref vs  │
                 │   working tree)       │
                 └──────────┬────────────┘
                            │ diffs by file
                            ▼
                 ┌─────────────────────┐        ┌──────────────────────┐
                 │   JavaAstAnalyzer   │──────▶ │   CallGraph          │
                 │   (JavaParser +     │        │   (callers/callees   │
                 │   symbol solver)    │        │   by canonical id)   │
                 └─────────────────────┘        └──────────┬───────────┘
                                                           │
                                                           ▼
                 ┌────────────────────────────────────────────────────────┐
                 │   ReviewContextBuilder                                 │
                 │   combines diffs + affected call-graph slice + source  │
                 │   into a single prompt (reviewPrompt.txt template)     │
                 └──────────┬─────────────────────────────────────────────┘
                            │ prompt
                            ▼
                 ┌─────────────────────┐
                 │   GeminiClient      │
                 │   (Gemini REST API) │
                 └──────────┬──────────┘
                            │ raw response
                            ▼
                 ┌─────────────────────┐
                 │   ReviewParser      │
                 │   (FILE/SEVERITY/   │
                 │   LOCATION/MESSAGE) │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │   ReviewPrinter     │
                 │   (colorized console│
                 │   output)           │
                 └─────────────────────┘
```

The call graph is built once for the entire source root, independent of the diff. `ReviewContextBuilder` then intersects the two: it takes every method whose file appears in the diff, pulls in its callers and callees from the graph, and attaches the source for each so Gemini can reason about effects that cross file boundaries.

## Quick Start

```bash
git clone https://github.com/SahilShahare/code-review-agent.git
cd code-review-agent
mvn clean package
export GEMINI_API_KEY=your-api-key-here
```

Run against a target repository:

```bash
java -jar target/code-review-agent-1.0.0-shaded.jar /path/to/target-repo src/main/java
```

## Usage

```bash
java -jar code-review-agent.jar <repo-path> <src-root> [options]
```

**Positional arguments (required):**

| Argument     | Description                                              |
|--------------|------------------------------------------------------------|
| `repo-path`  | Path to the Git repository to review                       |
| `src-root`   | Source root to parse, relative to `repo-path`               |

**Options:**

| Flag                  | Description                                   | Default            |
|-----------------------|-------------------------------------------------|---------------------|
| `-m`, `--model`       | Gemini model id to use for review generation     | `gemini-3.5-flash`  |
| `-b`, `--base-ref`    | Git ref to diff against the current working tree | `HEAD`              |

Examples:

```bash
java -jar code-review-agent.jar . src/main/java
java -jar code-review-agent.jar . src/main/java -m gemini-3.6-flash
java -jar code-review-agent.jar . src/main/java -b HEAD~1
java -jar code-review-agent.jar . src/main/java -m gemini-2.5-pro -b HEAD~1
```

The diff compares the given ref's tree against the current working tree (i.e. it includes uncommitted changes), scoped to `src-root`. If there are no changed files, the tool logs that and exits without calling Gemini.

## Configuration

| Variable            | Description                                                          | Required |
|----------------------|-------------------------------------------------------------------------|----------|
| `GEMINI_API_KEY`     | API key used to authenticate requests to the Gemini API                  | Yes      |
| `CODE_REVIEW_DEBUG`  | Set to `1` or `true` to log each unresolved call/instantiation the AST analyzer skips | No       |

Supported model ids (pass via `-m`/`--model`): `gemini-3.6-flash`, `gemini-3.5-flash` (default), `gemini-3.1-pro-preview`, `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite`, `gemini-3-flash-preview`, `gemini-2.5-pro`, `gemini-2.5-flash-lite`.

## Testing and Coverage

Run the test suite (JUnit 5, JUnit Pioneer, Mockito):

```bash
mvn test
```

Generate a JaCoCo coverage report:

```bash
mvn clean verify
```

The report is written to `target/site/jacoco/index.html`.

## Project Structure

```
code-review-agent/
├── pom.xml
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/codereview/
    │   │   ├── Main.java
    │   │   ├── analyzer/
    │   │   ├── builder/
    │   │   ├── client/
    │   │   ├── constants/
    │   │   ├── factory/
    │   │   ├── model/
    │   │   │   └── enums/
    │   │   └── util/
    │   └── resources/
    │       └── prompts/
    └── test/
        └── java/com/codereview/
```

| Path                        | Contents                                                                |
|------------------------------|-------------------------------------------------------------------------|
| `Main.java`                  | CLI entry point: argument parsing and orchestration                     |
| `analyzer/`                   | `GitAnalyzer` (JGit diff), `AstAnalyzer` + `JavaAstAnalyzer`            |
| `builder/`                    | `ReviewContextBuilder` — assembles the Gemini prompt                    |
| `client/`                     | `LLMClient` interface, `GeminiClient` (REST calls)                      |
| `constants/`                  | `Constants` — defaults, endpoints, ANSI codes                           |
| `factory/`                    | `AstAnalyzerFactory`, `LLMClientFactory`, signature factories           |
| `model/`                      | `CallGraph`, records (`MethodSignature`, `Finding`, `ParseResult`, ...) |
| `model/enums/`                | `Language`, `Severity`, `GeminiModel`, `LLMRegistry`                    |
| `util/`                       | `Logger`, `ReviewParser`, `ReviewPrinter`, `TerminalColors`             |
| `resources/prompts/`          | `ReviewPrompt.txt` — Gemini prompt template                             |
| `test/java/com/codereview/`   | JUnit 5 test suite, mirrors the main package structure                  |

## Limitations

- Supports Java source only; other languages are not analyzed
- Only one AI provider (Gemini) is supported
- Symbol resolution depends on JavaParser's symbol solver being able to see the relevant types; calls into dependencies outside the parsed source tree (e.g. external JARs) are skipped and logged, not resolved
- The diff always compares a base ref against the current working tree, not two arbitrary commits — there is no way to diff two fixed refs against each other
- Review quality and finding accuracy depend on the underlying Gemini model's output
- No CI/CD or GitHub Actions integration is included; this is a local CLI tool

## Roadmap

- Support for diffing two arbitrary refs instead of always diffing against the working tree
- Additional LLM providers beyond Gemini
- Support for languages beyond Java
- CI integration (e.g. a GitHub Action) for running the review as part of a pull request workflow

## Contributing

Contributions are welcome. Please open an issue to discuss significant changes before submitting a pull request. For smaller fixes:

1. Fork the repository
2. Create a feature branch
3. Run `mvn test` to confirm existing tests pass
4. Submit a pull request with a clear description of the change

## License

This project is licensed under the MIT License.