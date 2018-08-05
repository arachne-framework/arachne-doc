(ns arachne.doc-test
  (:require [arachne.core :as a]
            [arachne.core.descriptor :as d]
            [clojure.test :refer :all]
            [arachne.aristotle.registry :as reg]))

(reg/prefix :test "http://example.com/test/")

(def test-descriptor
  [{:rdf/about :test/rt
    :arachne.runtime/components {:rdf/about :test/c1
                                 :test/foo 32
                                 :arachne.component/constructor 'foo/quiz}}])

(deftest doc-test
  (let [d (a/descriptor :org.arachne-framework/arachne-doc test-descriptor)
        module-doc (ffirst (d/query d '[?doc]
                             '[:bgp [:org.arachne-framework/arachne-doc :arachne/doc ?doc]]))
        property-doc (ffirst (d/query d '[?doc]
                               '[:bgp [:arachne/doc :arachne/doc ?doc]]))]
    (is (re-find #"snippets" module-doc))
    (is (re-find #"documentation" property-doc))))
