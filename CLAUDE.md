# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

instaparse-bb is a Babashka wrapper library for [pod-babashka-instaparse](https://github.com/babashka/pod-babashka-instaparse). It provides a Babashka-compatible API that mirrors `instaparse.core` from the JVM [instaparse](https://github.com/Engelberg/instaparse) library. The library uses `.bb` file extensions so it can coexist on the classpath with the JVM instaparse without conflicts.

## Running Tests

```bash
bb test.clj
```

The test file (`test.clj`) uses assertions (not a test framework). It tests parsing, transform (both hiccup and enlive formats), `failure?`, `defparser`, file-based grammars, and IFn invocation of parsers.

## Architecture

- **`src/instaparse/core.bb`** - The entire library in a single file. It:
  1. Loads the `org.babashka/instaparse` pod via `babashka.pods`
  2. Delegates `parser`, `parse`, `parses`, `failure?`, `span` to the pod
  3. Implements `transform` locally (not in the pod) to support passing Clojure functions — handles both hiccup (`[:tag ...]` vectors) and enlive (`{:tag :foo :content [...]}`) formats
  4. Wraps pod parsers in a `reify` implementing `IFn` and a `Parser` protocol so parsers are directly callable
  5. `defparser` is a macro that pre-compiles string grammars at macro-time

- **`bb.edn`** / **`deps.edn`** - Both just `{:paths ["src"]}`. No external deps beyond the pod.

## Key Design Decisions

- `transform` must run client-side (not in the pod) because transform maps contain arbitrary Clojure functions that can't be serialized across the pod boundary
- Grammar strings and file paths (including `java.net.URL`) are resolved to strings before being sent to the pod via `slurp`
- The `Parser` protocol exposes `pod-ref` to access the underlying pod reference when needed
