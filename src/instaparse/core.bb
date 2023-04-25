(ns instaparse.core
  (:require [babashka.pods :as pods]))

(pods/load-pod
 ;; for local dev:
 #_["clojure" "-Sdeps" "{:deps {ip/ip {:local/root \"/Users/borkdude/dev/pod-babashka-instaparse\"}}}" "-M" "-m" "pod.babashka.instaparse"]
 'org.babashka/instaparse "0.0.3")

(require '[pod.babashka.instaparse :as insta])

(defn- map-preserving-meta [f l]
  (with-meta (map f l) (meta l)))

(defn- merge-meta-
  "A variation on with-meta that merges the existing metamap into the new metamap,
rather than overwriting the metamap entirely."
  [obj metamap]
  (with-meta obj (merge metamap (meta obj))))

(defn- merge-meta
  "This variation of the merge-meta in gll does nothing if obj is not
something that can have a metamap attached."
  [obj metamap]
  (if (instance? clojure.lang.IObj obj)
    (merge-meta- obj metamap)
    obj))

(defn- throw-illegal-argument-exception
  [& message]
  (let [^String text (apply str message)]
    (-> text
        IllegalArgumentException.
        throw)))

(defn- enlive-transform
  [transform-map parse-tree]
  (let [transform (transform-map (:tag parse-tree))]
    (cond
      transform
      (merge-meta 
       (apply transform (map (partial enlive-transform transform-map)
                             (:content parse-tree)))
       (meta parse-tree))
      (:tag parse-tree)
      (assoc parse-tree :content (map (partial enlive-transform transform-map)
                                      (:content parse-tree)))
      :else
      parse-tree)))

(defn- hiccup-transform
  [transform-map parse-tree]
  (if (and (sequential? parse-tree) (seq parse-tree))
    (if-let [transform (transform-map (first parse-tree))]
      (merge-meta
        (apply transform (map (partial hiccup-transform transform-map)
                              (next parse-tree)))
        (meta parse-tree))
      (with-meta 
        (into [(first parse-tree)]
              (map (partial hiccup-transform transform-map) 
                   (next parse-tree)))
        (meta parse-tree)))
    parse-tree))

;; Public functions

(defn parser [& args]
  (apply insta/parser args))

(defn parse [& args]
  (apply insta/parse args))

(defn failure? [& args]
  (apply insta/failure? args))

(defmacro defparser
  "Replicates the call semantics of the `defparser` macro from instaparse.
   String specifications are processed at macro-time, offering a performance boost."
  [name grammar & opts]
  (if (string? grammar)
    (let [[id parser-ref] (first (apply parser grammar opts))]
      `(def ~name {~id (quote ~parser-ref)}))
    `(def ~name (parser ~grammar `@opts))))

(defn transform
  "Replicates the `transform` function from instaparse."
  [transform-map parse-tree]
                                        ; Detect what kind of tree this is
  (cond
    (string? parse-tree)
                                        ; This is a leaf of the tree that should pass through unchanged
    parse-tree

    (and (map? parse-tree) (:tag parse-tree))
                                        ; This is an enlive tree-seq
    (enlive-transform transform-map parse-tree)
    
    (and (vector? parse-tree) (keyword? (first parse-tree)))
                                        ; This is a hiccup tree-seq
    (hiccup-transform transform-map parse-tree)
    
    (sequential? parse-tree)
                                        ; This is either a sequence of parse results, or a tree
                                        ; with a hidden root tag.
    (map-preserving-meta (partial transform transform-map) parse-tree)
    
    (insta/failure? parse-tree)
                                        ; pass failures through unchanged
    parse-tree
    
    :else
    (throw-illegal-argument-exception
     "Invalid parse-tree, not recognized as either enlive or hiccup format.")))
