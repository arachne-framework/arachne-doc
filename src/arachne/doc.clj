(ns arachne.doc
  (:require [arachne.doc.rdf-md]
            [arachne.core :as a]
            [arachne.core.descriptor :as d]
            [arachne.aristotle.graph :as g]
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

(comment

  (require 'arachne.aristotle.registry)
  (arachne.aristotle.registry/prefix :test "http://example.com/test/")

  (def data
    [{:rdf/about :test/rt
      :arachne.runtime/components {:rdf/about :test/c1
                                   :test/foo 32
                                   :arachne.component/constructor 'foo/quiz}}])

  (require '[clojure.edn :as edn])

  (def d (a/descriptor :org.arachne-framework/arachne-doc data true))
  (d/query d '[:bgp [?subj :arachne/doc ?d]])




  )
