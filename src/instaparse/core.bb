(ns instaparse.core
  (:require [babashka.pods :as pods]))

(pods/load-pod
 ;; for local dev:
 #_["clojure" "-Sdeps" "{:deps {ip/ip {:local/root \"/Users/borkdude/dev/pod-babashka-instaparse\"}}}" "-M" "-m" "pod.babashka.instaparse"]
 'org.babashka/instaparse "0.0.2")

(require '[pod.babashka.instaparse :as insta])

(defn parser [& args]
  (apply insta/parser args))

(defn parse [& args]
  (apply insta/parse args))

(defn failure? [& args]
  (apply insta/failure? args))
