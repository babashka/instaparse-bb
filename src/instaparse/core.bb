(ns instaparse.core
  (:require [babashka.pods :as pods]))

(pods/load-pod 'org.babashka/instaparse "0.0.1")

(require '[pod.babashka.instaparse :as insta])

(defn parser [& args]
  (apply insta/parser args))

(defn parse [& args]
  (apply insta/parse args))

(defn failure? [& args]
  (apply insta/failure? args))
