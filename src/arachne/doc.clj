(ns arachne.doc
  (:require [arachne.doc.rdf-md]
            [arachne.doc.format :as doc-format]
            [arachne.core :as a]
            [arachne.error.format :as fmt]
            [arachne.error :as err]
            [arachne.core.descriptor :as d]
            [arachne.aristotle.graph :as g]
            [io.aviso.ansi :as ansi]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.file :as file]
            [clojure.java.classpath :as cp]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- doc-files
  "Search the classpath and return a list of documentation files."
  []
  (let [doc-filename? #(str/ends-with? % ".rdf.md")
        docs-in-jars (->> (cp/classpath-jarfiles)
                       (mapcat cp/filenames-in-jar)
                       (filter doc-filename?)
                       (map io/resource)
                       (map io/file))
        docs-in-dirs (->> (cp/classpath-directories)
                       (mapcat file-seq)
                       (filter #(and (.isFile %)
                                  (doc-filename? (.getName %)))))]
    (concat docs-in-jars docs-in-dirs)))

(defn search-and-install
  "Configure function: Inspect the classpath and add all discovered
  docs (in *.rdf.md files) to the descriptor."
  [d]
  (doseq [doc-file (doc-files)]
    (d/add-file! d doc-file)))

(def ^{:doc "Print the documentation for a given resource in the descriptor. Options are:

   :color - print using ANSI colors (default true)"}
  doc doc-format/doc)

(comment

  (require 'arachne.aristotle.registry)
  (arachne.aristotle.registry/prefix :test "http://example.com/test/")
  (arachne.aristotle.registry/prefix :test.* "http://example.com/test/*/")

  (def data
    [{:rdf/about :arachne/Component
      :rdfs/comment "It's a whole thing."}
     {:rdf/about :test/rt
      :arachne.runtime/components {:rdf/about :test/c1
                                   :test/foo 32
                                   :arachne.component/constructor 'foo/quiz}}

     #arachne/class [:test/Animal []
                     "An animal"
                     :test.animal/species :one :required :xsd/string "species"]

     #arachne/class [:test/Mammal [:test/Animal]
                     "A mamal"
                     :test.mammal/fur-type :one :required :test/FurType "The type of fur of the animal"]

     #arachne/class [:test/Reptile [:test/Animal]
                     "A reptile"
                     :test.reptile/scale-type :one :required :test/ScaleType "The type of scale of the animal"]

     #arachne/class [:test/Snake [:test/Reptile]
                     "A reptile"
                       :test.snake/venomous :one :required :xsd/boolean "poisonous?"]])



  (require '[clojure.edn :as edn])

  (def d (a/descriptor :org.arachne-framework/arachne-doc data true))
  (d/query d '[:bgp [?subj :arachne/doc ?d]])

  (doc d :org.arachne-framework/arachne-doc)
  (doc d :org.arachne-framework/arachne-doc)

  (doc d :arachne.component/constructor)

  (doc d :arachne/Component)
  (doc d :arachne/Provenance)
  (doc d :clojure/Namespace)

  (doc d :test/Animal)
  (doc d :test/Reptile)
  (doc d :test/Snake)


  (import '[org.apache.jena.vocabulary ReasonerVocabulary])

  (str ReasonerVocabulary/directRDFType)

  (def sc '_f5e38418-9431-4f6f-9c77-2dc3af29d1fe)

  (doc d sc)


  )




