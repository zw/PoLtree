; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;
; Basic command-line interface.
;
(ns uk.me.iwilcox.poltree.cli
    (:require [clojure.string :as str])
    (:require [uk.me.iwilcox.poltree.s11n :as s11n])
    (:require [uk.me.iwilcox.poltree.core :as core])
    (:gen-class :main true))

(def usage (str/triml "
Usage:
  Generate a complete tree from a given accounts.json:
    poltree.sh completetree accounts.json
"))
;  Extract a customer's partial tree from a complete tree produced earlier by
;  completetree:
;    poltree.sh partialtree 'foo@example.com' completetree.json
; This does NOT complain if you feed it balances with excessive
; numbers of decimal places for the currency.

(declare slurp-file-or-stdin! complete-tree partial-tree root)

(defn -main [& args]
    (if-not args
        (println usage)
        (condp = (first args)
            "completetree" (apply complete-tree (rest args))
            "partialtree"  (apply partial-tree (rest args))
            "root"         (apply root (rest args))
            (println usage))))

(defn- complete-tree
  [& accounts-filename]
    (-> (slurp-file-or-stdin! accounts-filename)
        s11n/accounts-json->maps
        (core/accounts->tree true)
        s11n/tree->json
        println))

(defn- partial-tree
  [uid & wholetree-filename]
    (if (nil? uid)
        (println usage)
        (-> (slurp-file-or-stdin! wholetree-filename)
            s11n/tree-json->nodes
            ; FIXME: no protection against non-existent uid yet.
            (core/verification-path uid)
            s11n/vpath->json
            println)))

(defn- root
  [& wholetree-filename]
    (-> (slurp-file-or-stdin! wholetree-filename)
        s11n/tree-json->root
        println))

(defn- slurp-file-or-stdin! [& args]
    (if-let [filename (first args)]
        (if (.exists (clojure.java.io/as-file filename))
            (slurp filename :encoding "UTF-8")
            (println usage))
        (slurp *in* :encoding "UTF-8")))
