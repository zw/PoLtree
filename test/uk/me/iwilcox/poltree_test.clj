; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;
; Tests for uk.me.iwilcox.poltree.*
;
(ns uk.me.iwilcox.poltree_test
    (:require [clojure.test :refer [deftest is use-fixtures run-tests]])
    (:require [uk.me.iwilcox.poltree.core :as core])
    (:require [uk.me.iwilcox.poltree.s11n :as s11n]))

; If there's some recommended/typical/accepted way to provide bindings for
; fixtures, I can't find it documented anywhere.  This works, but I'd like
; sequential bindings like 'let'.
(def ^:dynamic *vpath3*)
(def ^:dynamic *vpath1-neg*)
(def ^:dynamic *vpath3-neg*)
; Useful for diagnosing failing tests
;(def ^:dynamic *tree*)
;(def ^:dynamic *tree-neg*)

(defn noddy-fixture [f]
    (let [accounts (map #(hash-map :uid % :balance (bigdec %) :nonce %)
                        ["0" "1" "2" "3" "4" "5"])
          tree (core/accounts->tree accounts true)
          index (core/index-leaves tree)
          vpath3 (core/verification-path tree (get index "3"))

          ; Evil accounts list: one account with a negative balance causing a
          ; negative internal node visible to user 3 but hidden from user 1.
          accounts-neg (map #(hash-map :uid %
                                       :balance (bigdec (if (= "2" %) -2 %))
                                       :nonce %)
                            ["0" "1" "2" "3" "4" "5"])
          tree-neg (core/accounts->tree accounts-neg true)
          index-neg (core/index-leaves tree-neg)
          vpath1-neg (core/verification-path tree-neg (get index "1"))
          vpath3-neg (core/verification-path tree-neg (get index "3"))]
        (binding [*vpath3* vpath3
                  *vpath1-neg* vpath1-neg
                  ;*tree* tree
                  ;*tree-neg* tree-neg
                  *vpath3-neg* vpath3-neg]
            (f))))
 
(deftest included
    (is (core/included? "3" 3M *vpath3* "f3de0d8cebdbea80a0fb393f60da20dea382d2567ba40a0574e6f0f1155a94bc")
        "account should verify as included in the root")
    (is (false? (core/included? "2" 3M *vpath3* "f3de0d8cebdbea80a0fb393f60da20dea382d2567ba40a0574e6f0f1155a94bc"))
        "inclusion verification should fail on UID mismatch")
    (is (false? (core/included? "3" 2M *vpath3* "f3de0d8cebdbea80a0fb393f60da20dea382d2567ba40a0574e6f0f1155a94bc"))
        "inclusion verification should fail on balance mismatch")
    (is (false? (core/included? "3" 3M *vpath3* "63c0e5d695e959c0c79093b54b2ddafe0dd28a20a47eab48d17768f9db966d97"))
        "inclusion verification should fail if published/computed root hashes differ")

    ; Swap our balance with that of our sibling.
    (let [sibling-bal (get-in (vec *vpath3*) [1 1 :sum])
          vpath3-balswap (assoc-in (vec *vpath3*) [1 1 :sum] 3M)]
        (is (false? (core/included? "3" sibling-bal vpath3-balswap "f3de0d8cebdbea80a0fb393f60da20dea382d2567ba40a0574e6f0f1155a94bc"))
            "inclusion verification should fail if sibling balances swap places"))

    (is (core/included? "1" 1M *vpath1-neg* "85bfcc7f40288d5288d7aaa7225dbdd682eed0f67ae48577c0ea8ac520ce6aa2")
        "inclusion verification should pass despite a hidden negative balance")
    (is (false? (core/included? "3" 3M *vpath3-neg* "85bfcc7f40288d5288d7aaa7225dbdd682eed0f67ae48577c0ea8ac520ce6aa2"))
        "inclusion verification should fail if a negative balance is visible"))

; More stuff to test:
;  - anything but sum/hash in siblings?

(use-fixtures :each noddy-fixture)

(run-tests)
