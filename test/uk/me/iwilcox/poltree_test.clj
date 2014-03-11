; Tests for uk.me.iwilcox.poltree/*.
;
; Copyright 2014 Isaac Wilcox.
; Distributed under the Boost Software License, Version 1.0.  See accompanying
; file LICENCE.txt or copy at <http://www.boost.org/LICENSE_1_0.txt>.

(ns uk.me.iwilcox.poltree_test
    (:require [clojure.test :refer [deftest is use-fixtures run-tests]])
    (:require [uk.me.iwilcox.poltree :as poltree]))

; If there's some recommended/typical/accepted way to provide bindings for
; fixtures, I can't find it documented anywhere.  This works, but I'd like
; sequential bindings like 'let'.
(def ^:dynamic vpath3)
(def ^:dynamic vpath1-neg)
(def ^:dynamic vpath3-neg)

(defn noddy-fixture [f]
    (let [accounts (map #(hash-map :uid % :balance % :nonce %) (range 6))
          tree (poltree/accounts->tree accounts)
          index (poltree/index-leaves tree)
          vpath3 (poltree/verification-path tree (get index 3))

          ; Evil accounts list: one account with a negative balance causing a
          ; negative internal node visible to user 3 but hidden from user 1.
          accounts-neg (map #(hash-map :uid % :balance (if (= 2 %) -2 %) :nonce %) (range 6))
          tree-neg (poltree/accounts->tree accounts-neg)
          index-neg (poltree/index-leaves tree-neg)
          vpath1-neg (poltree/verification-path tree-neg (get index 1))
          vpath3-neg (poltree/verification-path tree-neg (get index 3))]
        (binding [vpath3 vpath3
                  vpath1-neg vpath1-neg
                  vpath3-neg vpath3-neg]
            (f))))
 
(deftest included
    (is (poltree/included? 3 3 vpath3 "Nmu5CwNXrw2xiH2cKj4zyHbRWmoPAU3vZTt2RLVLS2g=")
        "account should verify as included in the root")
    (is (false? (poltree/included? 2 3 vpath3 "Nmu5CwNXrw2xiH2cKj4zyHbRWmoPAU3vZTt2RLVLS2g="))
        "inclusion verification should fail on UID mismatch")
    (is (false? (poltree/included? 3 2 vpath3 "Nmu5CwNXrw2xiH2cKj4zyHbRWmoPAU3vZTt2RLVLS2g="))
        "inclusion verification should fail on balance mismatch")
    (is (false? (poltree/included? 3 3 vpath3 "2yoFuH7ZtT36dP7zwPH08v4eD6BDwaiLt3Xn37Wvgyk="))
        "inclusion verification should fail if published/computed root hashes differ")

    (is (poltree/included? 1 1 vpath1-neg "ITGZ3LrynrYcUI8VK0h4pZtR0MH/g56l26IZPe6gqfs=")
        "inclusion verification should pass despite a hidden negative balance")
    (is (false? (poltree/included? 3 3 vpath3-neg "ITGZ3LrynrYcUI8VK0h4pZtR0MH/g56l26IZPe6gqfs="))
        "inclusion verification should fail if a negative balance is visible"))

; More stuff to test:
;  - anything but sum/hash in siblings?

(use-fixtures :each noddy-fixture)

(run-tests)
