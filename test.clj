#!/usr/bin/env bb

(require '[instaparse.core :as insta]
         '[clojure.string :as str])

(def as-and-bs
  (insta/parser
   "S = AB*
    AB = A B
    A = 'a'+
    B = 'b'+"))

(assert (= [:S [:AB [:A "a" "a" "a" "a" "a"] [:B "b" "b" "b"]] [:AB [:A "a" "a" "a" "a"] [:B "b" "b"]]] (insta/parse as-and-bs "aaaaabbbaaaabb")))

(def failure (insta/parse as-and-bs "xaaaaabbbaaaabb"))

(assert (insta/failure? failure) "should be true")

(def commit-msg-grammar
  "A PEG grammar to validate and parse conventional commit messages."
  (str
    "<S>            =       (HEADER <EMPTY-LINE> FOOTER GIT-REPORT? <NEWLINE>*)
                            / ( HEADER <EMPTY-LINE> BODY (<EMPTY-LINE> FOOTER)? GIT-REPORT? <NEWLINE>*)

                            / (HEADER <EMPTY-LINE> BODY GIT-REPORT? <NEWLINE>*)
                            / (HEADER GIT-REPORT? <NEWLINE>*);"
    "<HEADER>       =       TYPE (<'('>SCOPE<')'>)? <':'> <SPACE> SUBJECT;"
    "TYPE           =       'feat' | 'fix' | 'refactor' | 'perf' | 'style' | 'test' | 'docs' | 'build' | 'ops' | 'chore';"
    "SCOPE          =       #'[a-zA-Z0-9]+';"
    "SUBJECT        =       TEXT ISSUE-REF? TEXT? !'.';"
    "BODY           =       (!PRE-FOOTER PARAGRAPH) / (!PRE-FOOTER PARAGRAPH (<EMPTY-LINE> PARAGRAPH)*);"
    "PARAGRAPH      =       (ISSUE-REF / TEXT / (NEWLINE !NEWLINE))+;"
    "PRE-FOOTER     =       NEWLINE+ FOOTER;"
    "FOOTER         =       FOOTER-ELEMENT (<NEWLINE> FOOTER-ELEMENT)*;"
    "FOOTER-ELEMENT =       FOOTER-TOKEN <':'> <WHITESPACE> FOOTER-VALUE;"
    "FOOTER-TOKEN   =       ('BREAKING CHANGE' (<'('>SCOPE<')'>)?) / #'[a-zA-Z\\-^\\#]+';"
    "FOOTER-VALUE   =       (ISSUE-REF / TEXT)+;"
    "GIT-REPORT     =       (<EMPTY-LINE> / <NEWLINE>) COMMENT*;"
    "COMMENT        =       <'#'> #'[^\\n]*' <NEWLINE?> ;"
    "ISSUE-REF      =       <'#'> ISSUE-ID;"
    "ISSUE-ID       =       #'([A-Z]+\\-)?[0-9]+';"
    "TEXT           =       #'[^\\n\\#]+';"
    "SPACE          =       ' ';"
    "WHITESPACE     =       #'\\s';"
    "NEWLINE        =       <'\n'>;"
    "EMPTY-LINE     =       <'\n\n'>;"))

(def commit-msg-parser-hiccup (insta/parser commit-msg-grammar))

(def commit-msg-parser-enlive (insta/parser commit-msg-grammar :output-format :enlive))

(assert (= '([:TYPE "feat"] [:SUBJECT [:TEXT "adding a new awesome feature"]])
           (insta/parse commit-msg-parser-hiccup "feat: adding a new awesome feature")))

(assert (= '("feat::" "ADDING A NEW AWESOME FEATURE")
         (insta/transform {:TEXT reverse
                           :SUBJECT (comp str/reverse str/upper-case str/join)
                           :TYPE (fn [t] (str t "::"))}
                       (insta/parse commit-msg-parser-hiccup "feat: adding a new awesome feature"))))

;; test enlive

(assert (= '({:tag :TYPE, :content ("feat")}
 {:tag :SUBJECT,
  :content ({:tag :TEXT, :content ("adding a new awesome feature")})})
           (insta/parse commit-msg-parser-enlive "feat: adding a new awesome feature")))

(assert (= '("feat::" "ADDING A NEW AWESOME FEATURE")
         (insta/transform {:TEXT reverse
                           :SUBJECT (comp str/reverse str/upper-case str/join)
                           :TYPE (fn [t] (str t "::"))}
                       (insta/parse commit-msg-parser-enlive "feat: adding a new awesome feature"))))

;; test defparser

(defn tidy-string [s]
  (-> s str/trim-newline (str/replace #"\"" "")))

(def msecs-reg #"^\d*\.?\d*$")

;; "A parser to read the output of `time`."
(insta/defparser elapsed-time-parser
  (str
   "<S> = <Preamable> Msecs <Postamble>;"
   "Preamable = \"Elapsed time: \";"
   "Postamble = \" msecs\";"
   "<Msecs> = #'[^ ]*'" ))

(defn read-time [s]
  (->> s
       tidy-string
       (insta/parse elapsed-time-parser)
       first
       Float.))

;; assert that most of the work is done at compile time with defparser when passed a string
(assert (> (read-time (with-out-str (time (insta/parser "S = A B; A = 'a'+; B = 'b'+"))))
           (* 10 (read-time (with-out-str (time (insta/defparser time-parser "S = A B; A = 'a'+; B = 'b'+")))))))


