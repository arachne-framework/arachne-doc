(ns arachne.doc.data
  "Tools for querying documentation and structue from a descriptor."
  (:require [arachne.core :as a]
            [arachne.core.descriptor :as d]
            [arachne.aristotle.graph :as g]
            [loom.graph :as loom]
            [loom.alg :as loom-alg]
            [clojure.string :as str])
  (:import [org.apache.jena.vocabulary ReasonerVocabulary]))

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

(defn get-properties
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

(defn get-property-classes
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

(defn direct-superclasses
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

(defn get-all-properties
  "Get the declared properties and cardinalities for the given class,
  and its ancestors."
  [d class]
  (get-properties d class)
  (concat (get-properties d class)
    (mapcat #(get-all-properties d %) (direct-superclasses d class))))

(defn get-superclasses
  "Get all the superclasses of a given class, in topological order."
  [d class]
  (let [build (fn build [c]
                (let [supers (direct-superclasses d c)]
                  (apply merge {c supers} (map build supers))))
        graph (loom/digraph (build class))]
    (filter #(not= class %) (loom-alg/topsort graph))))
