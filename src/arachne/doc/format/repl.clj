(ns arachne.doc.format.repl
  (:require [arachne.core :as a]
            [arachne.repl :as fmt]
            [arachne.doc.data :as data]
            [arachne.core.descriptor :as d]
            [arachne.aristotle.graph :as graph]
            [arachne.error :as err]
            [io.aviso.ansi :as ansi]
            [clojure.string :as str])
  (:import [org.apache.jena.vocabulary ReasonerVocabulary]
           [org.apache.commons.lang3 StringEscapeUtils]))

(def ^:private link-ref-re #"<\?(ref|link)(.+?)\?>")

(defn- replace-links
  "Replace `link` processing instructions with formated text."
  [text]
  (str/replace text link-ref-re
    (fn [[_ type match]]
      (let [iri (str/trim match)
            node (graph/node (if (= \: (first iri))
                               (read-string iri)
                               (str "<" iri ">")))]

        (if (= "link" type)
          (fmt/cfstr ansi/bold-yellow (str (graph/data node)))
          (str (graph/data node)))))))

(defn- print-docs
  "Format the :arachne/doc on an entity for output to the REPL"
  [entity]
  (let [docs (concat (:arachne/doc entity)
               (:rdfs/comment entity))]
    (when-not (empty? docs)
      (fmt/cfprint ansi/cyan "Documentation:\n\n")
      (->> docs
        (map str/trim)
        (str/join "\n\n")
        (replace-links)
        (StringEscapeUtils/unescapeXml)
        (fmt/colorize)
        (println))
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

(defn describe
  "Print the documentation for a given resource in the descriptor."
  [d iri & {:as opts}]
  (let [entity (d/pull d iri '[*])]
    (print-header d entity)
    (print-class d entity)
    (print-prop d entity)
    (print-docs entity)
    (print "\n\n")))
