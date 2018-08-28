(ns arachne.doc-test
  (:require [arachne.core :as a]
            [arachne.core.descriptor :as d]
            [arachne.doc :as doc]
            [arachne.doc.data :as data]
            [clojure.test :refer :all]
            [arachne.aristotle.registry :as reg]))

(reg/prefix :test "http://example.com/test/")
(reg/prefix :test.* "http://example.com/test/key/")

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

(def test-data
  [{:rdf/about :test/rt
    :arachne.runtime/components {:rdf/about :test/c1
                                 :test/foo 32
                                 :arachne.component/constructor 'foo/quiz}}
   {:rdf/about :test/claws
    :rdfs/range [:xsd/string :test/ClawType]
    :rdfs/domain [:test/Reptile :test/Mammal]}
   {:rdf/about :test/Mammal
    :rdfs/subClassOf {:rdf/type :owl/Restriction
                      :owl/onProperty :test/claws
                      :owl/minCardinality 4}}

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

(deftest test-get-properties
  (let [d (a/descriptor :org.arachne-framework/arachne-doc test-data)]
    (is (= (data/get-properties d :test/Animal)
          [{"Property" :test.animal/species,
            "Min" 1,
            "Max" 1,
            "Range" :xsd/string,
            "Domain" :test/Animal}]))
    (is (= (data/get-properties d :test/Reptile)
          [{"Property" :test/claws,
            "Min" "",
            "Max" "",
            "Range" :xsd/string,
            "Domain" :test/Reptile}
           {"Property" :test/claws,
            "Min" "",
            "Max" "",
            "Range" :test/ClawType,
            "Domain" :test/Reptile}
           {"Property" :test.reptile/scale-type,
            "Min" 1,
            "Max" 1,
            "Range" :test/ScaleType,
            "Domain" :test/Reptile}]))
    (is (= (data/get-properties d :test/Snake)
          [{"Property" :test.snake/venomous,
            "Min" 1,
            "Max" 1,
            "Range" :xsd/boolean,
            "Domain" :test/Snake}]))))

(deftest test-get-all-properties
  (let [d (a/descriptor :org.arachne-framework/arachne-doc test-data)]
    (is (= (data/get-all-properties d :test/Snake)
          [{"Property" :test.snake/venomous,
            "Min" 1, "Max" 1,
            "Range" :xsd/boolean,
            "Domain" :test/Snake}
           {"Property" :test/claws,
            "Min" "", "Max" "",
            "Range" :xsd/string,
            "Domain" :test/Reptile}
           {"Property" :test/claws,
            "Min" "", "Max" "",
            "Range" :test/ClawType,
            "Domain" :test/Reptile}
           {"Property" :test.reptile/scale-type,
            "Min" 1, "Max" 1,
            "Range" :test/ScaleType,
            "Domain" :test/Reptile}
           {"Property" :test.animal/species,
            "Min" 1, "Max" 1, "Range" :xsd/string,
            "Domain" :test/Animal}]))))

(deftest test-get-property-classes
  (let [d (a/descriptor :org.arachne-framework/arachne-doc test-data)]
    (is (= (data/get-property-classes d :test.animal/species)
          [{"Domain" :test/Animal,
            "Range" :xsd/string,
            "Min" 1, "Max" 1}]))
    (is (= (data/get-property-classes d :test/claws)
          [{"Domain" :test/Reptile, "Range" :xsd/string, "Min" "", "Max" ""}
           {"Domain" :test/Reptile, "Range" :test/ClawType, "Min" "", "Max" ""}
           {"Domain" :test/Mammal, "Range" :xsd/string, "Min" 4, "Max" ""}
           {"Domain" :test/Mammal, "Range" :test/ClawType, "Min" 4, "Max" ""}]))))
