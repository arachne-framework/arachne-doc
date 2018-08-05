(ns arachne.doc.rdf-edn
  "Reader/writer for RDF/MD"
  (:require [arachne.aristotle.graph :as g]
            [clojure.string :as str]
            [arachne.aristotle.registry :as reg])
  (:import [org.apache.jena.riot LangBuilder RDFParserRegistry ReaderRIOTFactory ReaderRIOT]
           [org.apache.jena.riot.system ParserProfile StreamRDF]
           [org.apache.jena.atlas.web ContentType]
           [org.apache.jena.sparql.util Context]
           [java.io InputStream Reader InputStreamReader]))

(def lang (-> (LangBuilder/create)
              (.langName "RDF/MD")
              (.contentType "text/markdown")
              (.addAltContentTypes (into-array String ["text/markdown"]))
              (.addFileExtensions (into-array String ["md"]))
              (.build)))

(def ^:private about-re #"(?s)<\?about(.*?)\?>")
(def ^:private prefix-re #"(?s)<\?prefix(.*?)=(.*?)\?>")
(def ^:private global-prefix-re #"(?s)<\?global\-prefix(.*?)=(.*?)\?>")

(defn- parse-iri
  "IRIs can be bare keywords or a string IRI"
  [iri]
  (let [iri (str/trim iri)]
    (if (= \: (first iri))
      (read-string iri)
      (str "<" iri ">"))))

(defn- extract-prefixes
  "For each <?prefix ?> processing instruction, return an Aristotle Prefix record."
  [s]
  (->> (re-seq prefix-re s)
    (map #(drop 1 %))
    (map (fn [[token iri]]
           (let [token (str/trim token)]
             [(if (= \: (first token))
                (read-string token)
                token)
              iri])))
    (map reg/read-prefix)))

(defn- extract-global-prefixes
  "For each <?global-prefix ?> processing instruction, register it
  globally and return an Aristotle Prefix record."
  [s]
  (->> (re-seq global-prefix-re s)
    (map #(drop 1 %))
    (map (fn [[token iri]]
           (let [token (str/trim token)]
             [(if (= \: (first token))
                (read-string token)
                token)
              iri])))
    (map reg/read-global-prefix)))

(defn- read-md
  "Read Markdown from an input stream or Reader into the given StreamRDF output object"
  [input ^StreamRDF output]
  (let [s (slurp input)
        prefixes (extract-prefixes s)
        global-prefixes (extract-global-prefixes s)
        topics (map second (re-seq about-re s))
        sections (drop 1 (str/split s about-re))
        data (map (fn [topic section]
                    {:rdf/about (parse-iri topic)
                     :arachne/doc section})
               topics sections)
        data (vec (concat global-prefixes prefixes data))]
    (doseq [t (g/triples data)]
      (.triple output t))
    (.finish output)))

(defn- riot-reader
  "Construct a new RIOT reader for Markdown"
  []
  (reify ReaderRIOT
    (^void read [this
                 ^InputStream is
                 ^String base
                 ^ContentType ct
                 ^StreamRDF output
                 ^Context context]
     (read-md is output))
     (^void read [this
                  ^Reader rdr
                  ^String base
                  ^ContentType ct
                  ^StreamRDF output
                  ^Context context]
      (read-md rdr output))))

(def ^:private factory (reify ReaderRIOTFactory
                         (create [_ lang profile]
                           (riot-reader))))

(RDFParserRegistry/registerLangTriples lang factory)
