(ns arachne.doc.format
  (:require [arachne.core :as a]
            [arachne.error.format :as fmt]
            [arachne.error :as err]
            [arachne.core.descriptor :as d]
            [arachne.aristotle.graph :as g]
            [loom.graph :as loom]
            [loom.alg :as loom-alg]
            [io.aviso.ansi :as ansi]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.file :as file]
            [clojure.java.classpath :as cp]
            [clojure.java.io :as io]
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
      (print "\n")
      )))

(def properties-q
  '[:conditional
    [:minus
     [:minus
      [:bgp [?property :rdfs/domain ?domain]
            [?property :rdfs/range ?range]]
      [:filter (not= ?range ?subrange)
       [:bgp [?property :rdfs/range ?range]
             [?subrange :rdfs/subClassOf ?range]]]]
     [:filter (not= ?domain ?subdomain)
      [:bgp [?property :rdfs/domain ?subdomain]
            [?subdomain :rdfs/subClassOf ?domain]]]]
    [:conditional
     [:bgp [?domain :rdfs/subClassOf ?restriction]
           [?restriction :owl/onProperty ?property]]
     [:disjunction [:bgp [?restriction :owl/cardinality ?card]]
      [:union [:bgp [?restriction :owl/minCardinality ?min-card]]
              [:bgp [?restriction :owl/maxCardinality ?max-card]]]]]])

(defn- get-properties
  "Given a class, return a map of the properties with that class as the
  declared domain (i.e not including superclasses.)

  Also returns the range and cardinality of the property. The range returned is
  the declared range (i.e not including superclasses.)"
  [d class]
  (let [results (d/query d properties-q {'?domain class})]
    (sort-by #(get % "Property")
      (for [result results]
        {"Property" (get result '?property)
         "Min" (or (get result '?card) (get result '?min-card) "")
         "Max" (or (get result '?card) (get result '?max-card) "")
         "Range" (get result '?range)
         "Domain" class}))))

(defn- get-property-classes
  "Given a property, return a map of the classes to which it can be
  applied (as direct declared domains, not including superclasses.)

  Also returns the range and cardinality of the property for each class. The range returned is
  the declared range (i.e not including superclasses.)"
  [d property]
  (let [results (d/query d properties-q {'?property property})]
    (sort-by #(get % "Class")
      (for [result results]
        {"Domain" (get result '?domain)
         "Range" (get result '?range)
         "Min" (or (get result '?card) (get result '?min-card) "")
         "Max" (or (get result '?card) (get result '?max-card) "")}))))

(def directSubClassOf (str "<" ReasonerVocabulary/directSubClassOf ">"))

(defn- direct-superclasses
  "Given a descriptor and a class, return all that classes direct
  superclasses."
  [d class]
  (let [results (d/query d '[?super]
                  '[:filter (and (not (isBlank ?super))
                                 (not= ?super ?class))
                    [:bgp [?class ?directSubClassOf ?super]]]
                  {'?class class
                   '?directSubClassOf directSubClassOf})]
    (->> results
      (map first)
      (filter #(not (#{:owl/Thing :rdfs/Resource} %))))))

(defn get-superclasses
  "Get all the superclasses of a given class, in order."
  [d class]
  (let [build (fn build [c]
                (let [supers (direct-superclasses d c)]
                  (apply merge {c supers} (map build supers))))
        graph (loom/digraph (build class))]
    (filter #(not= class %) (loom-alg/topsort graph))))

(defn- get-all-properties
  "Get the properties and cardinalities for the given class and its ancestors."
  [d class]
  (get-properties d class)
  (concat (get-properties d class)
    (mapcat #(get-all-properties d %) (direct-superclasses d class))))

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
                       (get-superclasses d))
          props (->> (:rdf/about entity)
                  (get-all-properties d))]
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
    (let [classes (get-property-classes d (:rdf/about entity))]
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

(defn doc
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
