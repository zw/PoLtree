; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; Distributed under the Boost Software License, Version 1.0.  See accompanying
; file LICENCE.txt or copy at <http://www.boost.org/LICENSE_1_0.txt>.
;
; Serialisation to/from formats described in section "Serialized data formats"
; of <https://github.com/olalonde/blind-liability-proof>.
;
(ns uk.me.iwilcox.poltree.s11n
    (:require [clojure.data.json :as json])
    (:require [clojure.set :as set])
    (:require [clojure.string :as str])
    (:require [clojure.walk :as walk])
    (:require [uk.me.iwilcox.poltree.core :as core]))

(def nonce-bits 128)

(declare add-missing-nonce vpath->json-helper)

; Verification-path is as returned by verification-path.
(defn vpath->json [ [ nonce & path ] ]
    (let [user-node { "data" { "nonce" nonce } } ]
        (-> (reduce vpath->json-helper user-node path)
            (json/write-str ,,,))))

(defn accounts-json->maps [accounts-json]
    (->> (json/read-str accounts-json :key-fn keyword)
         (map add-missing-nonce ,,,)))

(defn tree->json [tree]
    (json/write-str (walk/postwalk core/as-map tree)))

; TO DO: tree->root-json

;;;;;;;;;;;
; Helpers
;;;;;;;;;;;

(defn- make-nonce-hexstr [num-bytes]
    (let [sr (java.security.SecureRandom.)
          bytes (byte-array num-bytes)]
        (.nextBytes sr bytes)
        (str/join (map #(format "%02x" %) bytes))))

(defn- add-missing-nonce [account-map]
    (if-not (contains? account-map :nonce)
        (assoc account-map :nonce (make-nonce-hexstr (/ nonce-bits 8)))
        account-map))

(defn- vpath->json-helper [node [side sibling]]
    (let [sibling (set/rename-keys sibling {:sum "value" :hash "hash"})]
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
