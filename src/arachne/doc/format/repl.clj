(ns arachne.doc.format.repl
  (:require [arachne.core :as a]
            [arachne.error.format :as fmt]
            [arachne.doc.data :as data]
            [arachne.core.descriptor :as d]
            [arachne.error :as err]
            [io.aviso.ansi :as ansi]
            [clojure.string :as str])
  (:import [org.apache.jena.vocabulary ReasonerVocabulary]))

(defn- print-docs
  "Format the :arachne/doc on an entity for output to the REPL"
  [entity]
  (let [docs (concat (:arachne/doc entity)
               (:rdfs/comment entity))]
    (when-not (empty? docs)
      (fmt/cfprint ansi/cyan "Documentation:\n\n")
      (->> docs (map str/trim) (str/join "\n\n") (println))
      (print "\n"))))

(defn- print-table
  "Prints a collection of maps in a textual table. Code is derived from
  clojure.pprint/print-table, but left-aligned."
  ([ks rows]
     (when (seq rows)
       (let [widths (map
                     (fn [k]
                       (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                     ks)
             spacers (map #(apply str (repeat % "-")) widths)
             fmts (map #(str "%-" % "s") widths)
             fmt-row (fn [leader divider trailer row]
                       (str leader
                            (apply str (interpose divider
                                                  (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                                    (format fmt (str col)))))
                            trailer))]
         (println)
         (println (fmt-row "| " " | " " |" (zipmap ks ks)))
         (println (fmt-row "|-" "-+-" "-|" (zipmap ks spacers)))
         (doseq [row rows]
           (println (fmt-row "| " " | " " |" row))))))
([rows] (print-table (keys (first rows)) rows)))


(defn- print-class
  [d entity]
  (when (:rdfs/Class (:rdf/type entity))
    (let [superclasses (->> (:rdf/about entity)
                       (data/get-superclasses d))
          props (->> (:rdf/about entity)
                  (data/get-all-properties d))]
      (when-not (empty? superclasses)
        (fmt/cfprint ansi/cyan "Subclass Of: ")
        (println (str/join ", " superclasses) "\n"))
      (when-not (empty? props)
        (fmt/cfprint ansi/cyan "Domain Of:\n")
        (print-table props))
      (print "\n"))))

(defn- print-prop
  [d entity]
  (when (:rdf/Property (:rdf/type entity))
    (let [classes (data/get-property-classes d (:rdf/about entity))]
      (when-not (empty? classes)
    (fmt/cfprint ansi/cyan "Declared Domains:\n")
        (print-table classes)))
     (print "\n")))

(defn- print-header
  [d entity]
  (let [types (str/join ", " (:rdf/type entity))
        len (min 80 (+ (count types) 16))]
    (print "\n")
    (fmt/cfprint ansi/cyan (apply str (repeat len "=")) "\n")
    (fmt/cfprint ansi/cyan "Resource: ")
    (println (:rdf/about entity))
    (fmt/cfprint ansi/cyan "Inferred Types: ")
    (println types)
    (fmt/cfprint ansi/cyan (apply str (repeat len "-")) "\n")
    (print "\n")))

(def ^{:dynamic true
       :doc "Default options for doc formatting"}
   *default-doc-opts*
  {:color true})

(defn describe
  "Print the documentation for a given resource in the descriptor. Options are:

   :color - print using ANSI colors (default true)"
  [d iri & {:as opts}]
  (let [opts (merge *default-doc-opts* opts)
        entity (d/pull d iri '[*])]
    (binding [fmt/*color* (:color opts)]
      (print-header d entity)
      (print-class d entity)
      (print-prop d entity)
      (print-docs entity)
      (print "\n\n"))))
