# instaparse-bb

A library that loads and wraps [pod-babashka-instaparse](https://github.com/babashka/pod-babashka-instaparse).

This library can be safely included on your classpath alongside the regular
[instaparse](https://github.com/Engelberg/instaparse) without causing issues for
JVM Clojure, since it only has `.bb` files which do not conflict with `.clj`
files.

## Installation

Add this library to your `bb.edn` in the `:deps` map:

``` clojure
io.github.babashka/instaparse-bb {:git/sha "<latest-sha>"}
```

Replace `<latest-sha>` with the latest SHA of this repo or use a tagged version.

## API

Only a subset of instaparse is exposed. If you are missing functionality, please create an issue.

### instaparse.core

- `parser`
- `parse`
- `parses`
- `failure?`
- `transform`
- `defparser`

## Differences with instaparse

none for currently exposed functionality

## Example

``` clojure
(require '[instaparse.core :as insta])

(def as-and-bs
  (insta/parser
   "S = AB*
    AB = A B
    A = 'a'+
    B = 'b'+"))

(prn (insta/parse as-and-bs "aaaaabbbaaaabb"))

(prn (as-and-bs "aaaaabbbaaaabb"))

(def failure (insta/parse as-and-bs "xaaaaabbbaaaabb"))

(prn failure)

(prn :failure? (insta/failure? failure))
```

## License

Copyright © 2023 Michiel Borkent

Distributed under the EPL 1.0 license, same as Instaparse and Clojure. See LICENSE.
