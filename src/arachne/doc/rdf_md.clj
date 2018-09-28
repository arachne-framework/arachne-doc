(ns arachne.doc.rdf-edn
  "Reader/writer for RDF/MD"
  (:require [arachne.aristotle.graph :as g]
            [clojure.string :as str]
            [arachne.aristotle.registry :as reg])
  (:import [org.apache.jena.riot LangBuilder RDFParserRegistry ReaderRIOTFactory ReaderRIOT]
           [org.apache.jena.riot.system ParserProfile StreamRDF]
           [org.apache.jena.atlas.web ContentType]
           [org.apache.jena.sparql.util Context]
           [org.apache.jena.graph Node]
           [java.io InputStream Reader InputStreamReader]))

(def lang (-> (LangBuilder/create)
              (.langName "RDF/MD")
              (.contentType "text/markdown")
              (.addAltContentTypes (into-array String ["text/markdown"]))
              (.addFileExtensions (into-array String ["md"]))
              (.build)))

(def about-re #"(?s)<\?about(.*?)\?>")
(def prefix-re #"(?s)<\?prefix(.*?)=(.*?)\?>")
(def global-prefix-re #"(?s)<\?global\-prefix(.*?)=(.*?)\?>")
(def link-re #"<\?link(.*?)\?>")
(def ref-re #"<\?ref(.*?)\?>")

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

(defn- resolve-refs
  "Given a string, replace IRIs in `ref` processing instructions with
  their canonical string form."
  [s]
  (str/replace s ref-re
    (fn [[_ iri]]
      (str "<?ref " (.getURI (g/node (parse-iri iri))) " ?>"))))

(defn- resolve-links
  "Given a string, replace IRIs in `link` processing instructions with
  their canonical string form."
  [s]
  (str/replace s link-re
    (fn [[_ iri]]
      (str "<?link " (.getURI (g/node (parse-iri iri))) " ?>"))))

(defn- read-md
  "Read Markdown from an input stream or Reader into the given StreamRDF output object"
  [input ^StreamRDF output]
  (let [s (slurp input)
        prefixes (extract-prefixes s)
        global-prefixes (extract-global-prefixes s)]
    (reg/with {}
      (doseq [prefix (concat global-prefixes prefixes)]
        (reg/install-prefix prefix))
      (let [s (resolve-links s)
            s (resolve-refs s)
            topics (map second (re-seq about-re s))
            sections (drop 1 (str/split s about-re))
            data (map (fn [topic section]
                        {:rdf/about (parse-iri topic)
                         :arachne/doc section})
                   topics sections)]
        (doseq [t (g/triples data)]
          (.triple output t))))
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
