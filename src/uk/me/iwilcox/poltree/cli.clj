; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; Distributed under the Boost Software License, Version 1.0.  See accompanying
; file LICENCE.txt or copy at <http://www.boost.org/LICENSE_1_0.txt>.
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

(declare slurp-accounts)

(defn -main [& args]
    (if-not args
        (println usage)
        (condp = (first args)
            "completetree" (-> (second args)
                               (slurp-accounts ,,,)
                               (s11n/accounts-json->maps ,,,)
                               ; Future: add :deterministic true
                               (core/accounts->tree ,,, true)
                               (s11n/tree->json ,,,)
                               (println ,,,))
            (println usage))))

(defn- slurp-accounts [& args]
    (if-let [filename (first args)]
        (if (.exists (clojure.java.io/as-file filename))
            (slurp filename :encoding "UTF-8")
            (println usage))
        (slurp *in* :encoding "UTF-8")))
