; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;
; Serialisation to/from formats described in section "Serialized data formats"
; of <https://github.com/olalonde/proof-of-liabilities>.
;
(ns uk.me.iwilcox.poltree.s11n
    (:require [clojure.data.json :as json])
    (:require [clojure.set :as set])
    (:require [clojure.string :as str])
    (:require [clojure.walk :as walk])
    (:require [uk.me.iwilcox.poltree.core :as core])
    (:import  (java.lang NumberFormatException)))

(def sum-regex
  "When parsing `sum` key/value pairs in accounts lists, this is used
  to check that numbers follow the spec."
  #"(?x:
        (?:   0
            | [1-9] [0-9]* )
        # Fractional part, maybe
        (?: \. [0-9]+ )?
    )")

(declare vpath->json-helper adapt-json-account-map
         adapt-core-tree-node adapt-json-tree-node)

(defn vpath->json
  "Given a path as returned by core/verification-path, return a JSON
  representation of the partial tree conforming to the spec."
  [ [ nonce & path ] ]
    (let [user-node { "data" { "nonce" nonce } } ]
        (-> (reduce vpath->json-helper user-node path)
            json/write-str)))

; This does NOT complain if you feed it balances with excessive
; numbers of decimal places for the currency.
(defn accounts-json->maps [accounts-json]
    (->> (json/read-str accounts-json :key-fn keyword)
         (map adapt-json-account-map)))

(defn tree->json [tree]
    (json/write-str (walk/postwalk adapt-core-tree-node tree)))

(defn tree-json->nodes
  "Given a JSON fragment representing a tree from core/accounts->tree,
  reproduce the tree."
  [tree-json]
    (->> (json/read-str tree-json :key-fn keyword)
         (walk/postwalk adapt-json-tree-node)))

(defn- adapt-json-tree-node
  "Given a map element in a freshly JSON-parsed tree, return the
  corresponding internal representation.  `walk/postwalk`-compatible."
  [elem]
    (if (map? elem)
        (if (contains? elem :data)
            (->> (-> (update-in elem [:data :sum] bigdec)
                     (update-in [:data] #(set/rename-keys % {:user :uid})))
                 ((juxt :data :left :right))
                 (take-while identity))
            elem)
        elem))

; TO DO: tree->root-json

;;;;;;;;;;;
; Helpers
;;;;;;;;;;;
(defn- adapt-json-account-map
  "Given an account map freshly parsed from JSON, validate it then
  adapt it to what core/* expect and return the updated map."
  ; FIXME: could do more validation but at the moment it's a test
  ; format only and the stricter checks in core will catch it.
  [account]
    (if-not (re-matches sum-regex (:balance account))
        (throw (NumberFormatException.
                (format "\"balance\" value %s for user %s isn't supported by spec"
                       (:balance account) (:user account)))))
    (-> (update-in account [:balance] bigdec)
        (set/rename-keys {:user :uid})))

(defn- adapt-core-tree-node
  "Given a tree node from core, adapt it to a map conforming to the
  spec for JSON tree objects and return that.  Given anything part of
  a tree, return it untouched (making this `walk/*`-compatible)."
  [elem]
    (if (seq? elem) ; node?
        (-> (core/as-map elem)
            (update-in [:data] #(set/rename-keys % {:uid :user})))
        elem))

(defn- vpath->json-helper [node [side sibling]]
    (let [sibling (-> (set/rename-keys sibling {:sum "sum" :hash "hash"})
                      (update-in ["sum"] core/format-min-dp))]
        (if (= :left side)
            {
                "left" {
                    "data" sibling
                },
                "right" node
                ; "data" { <to be added/filled in during verification> }
            }
            ; else
            {
                "left" node,
                "right" {
                    "data" sibling
                }
                ; "data" { <to be added/filled in during verification> }
            })))
