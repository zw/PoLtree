; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;
; Core interface/data types.
;
(ns uk.me.iwilcox.poltree.core
    (:require [clojure.string :as str])
    (:require [clojure.set :as set])
    (:require [clojure.math.numeric-tower :as math])
    (:require [uk.me.iwilcox.poltree.util :as util]))

(declare node-data left-child right-child leaf?
         sha256-hex hcombine ncombine add-leaf-hash
         prep-account pp-accounts->tree
         combiner-reducer path-to-uid)

;;;;;;;;;;;;;;;;;;;;;;;;
; Coinholder interface
;;;;;;;;;;;;;;;;;;;;;;;;

; Automatically added nonces contain this many random bits.
(def nonce-bits 128)

; Build a binary Merkle tree of liabilities from a collection of
; account maps, each with (at least) the keys: :uid :nonce :balance.
; FIXME: check here or in pp-accounts->tree that there are no dupe
; uids in the list.
; This does NOT complain if you feed it balances with excessive
; numbers of decimal places for the currency.

(defn accounts->tree
  "Build a binary Merkle tree of liabilities from a `Sequential` of
  account maps, each with (at least) the keys :uid :nonce :balance.
  If `deterministic` is false (the default) the resulting tree will
  have a random shape and leaves will be visited in a random order if
  the tree is walked.  Otherwise the tree will be a perfect binary
  tree (padded with dummy accounts as per the spec) and traversal will
  return the leaves in the order supplied."
  ([accounts] (accounts->tree accounts false))
  ([accounts deterministic]
    ; For each account, adapt keys then nest in a list to make it a tree leaf
    ; node.
    (-> (map (comp list prep-account) accounts)
        (pp-accounts->tree deterministic)
        first)))
; FIXME: get rid of `first`; maybe you can not call partition-all, but
; just partition, in pp-accounts->tree-deterministic?

; Given a liability tree root, walk the tree and produce an index from account
; UID to directions-to-that-leaf.  The directions consist of a sequential
; thingy of :left/:right symbols indicating the child to pick at each non-leaf.
(defn index-leaves
  ([root] (index-leaves root []))
  ([node path]
    (if node
        (if (leaf? node)
            {(:uid (node-data node)) path}
            (merge (index-leaves (left-child node)  (conj path :left))
                   (index-leaves (right-child node) (conj path :right)))))))

; Given a liability tree root and directions-to-leaf, return nonce and siblings
; of nodes on the root path in deepest->shallowest order, like:
;
;     ( nonce [sibling-side sibling-data] [sibling-side sibling-data] ...)
;
; e.g. for leaf/customer node D with nonce 12345 in this tree:
;
;       A
;     /   \
;    B     E
;  /   \
; C     D
;
; the root path would be D, B, A and this function would return:
;
;     ( 12345  [:left { <C's data> }]  [:right { <E's data> }] )
;
; The customer's leaf data is deliberately omitted because having customers (or
; customer plugins/code) provide their own details removes the temptation to
; blindly include leaf data provided by the exchange.  Other data on the root
; path is omitted because it's redundant.  The root node is omitted because
; it's preferable for customers (/plugins/code) to obtain the root hash from
; the openly published source than to be tempted to expose themselves to a
; substitution attack by using a root provided by the exchange.
;
; Returned sibling node hashes contain only the keys  :sum  :hash.
;
; TODO: currently fails pretty silently if passed root=nil.
(defn verification-path
  ([root path-or-uid]
    (if (string? path-or-uid)
        (verification-path root (path-to-uid path-or-uid root) ())
        (verification-path root path-or-uid ())))
  ([node path-to-leaf vpath]
    (if (leaf? node)
        (cons (:nonce (node-data node)) vpath)
        (let [goleft (= :left (first path-to-leaf))
              follow-child ((if goleft left-child right-child) node)
              other-child-side (if goleft :right :left)
              other-child ((if goleft right-child left-child) node)
              other-child-data (select-keys (node-data other-child)
                                            [:sum :hash])]
            (->> (cons [other-child-side other-child-data] vpath)
                 (recur follow-child (rest path-to-leaf)))))))

;;;;;;;;;;;;;;;;;;;;;;
; Customer interface.
;;;;;;;;;;;;;;;;;;;;;;

; Given a minimal path-to-root and account details, determine whether the
; balance of that account was included in the root's total, and whether all
; balances seen on the path-to-root were non-negative.
(defn included? [uid balance [nonce & vpath] published-root-hash]
    ; Just barely resisting the urge to use keywordize:
    ;   https://github.com/amalloy/amalloy-utils/blob/master/src/amalloy/utils.clj#L30
    (let [account (prep-account {:uid uid :balance balance :nonce nonce})]
        (and (not-any? (comp neg? :sum second) vpath)
             (= published-root-hash
                (:hash (reduce combiner-reducer account vpath))))))

;;;;;;;;;;;;
; Utilities
;;;;;;;;;;;;
(defn format-min-dp
  "Format `sum` (a bigdec) with the minimum possible number of
  trailing zeros."
  [sum]
    ; Being careful to work around JDK bug 6480539 (fixed in JDK 8):
    ;   "BigDecimal.stripTrailingZeros() has no effect on zero itself ("0.0")"
    ;   http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6480539
    (if (zero? (compare BigDecimal/ZERO sum))
        "0"
        (.toPlainString (.stripTrailingZeros sum))))

;;;;;;;;;;;;;;;;;;;
; Internal Helpers
;;;;;;;;;;;;;;;;;;;
(declare add-nonce-if-missing make-nonce-hexstr validate-account-map
         pp-accounts->tree-deterministic pp-accounts->tree-random
         gaussian-split format-min-dp)

; Convenience bits for manipulating tree nodes of the form:
;     ( { <data> }  <left child>  <right child> )
(defn- node-data [n] (first n))
(defn- left-child [n] (second n))
(defn- right-child [n] (nth n 2 nil)) ; aka third
(defn- leaf? [n] (and (nil? (left-child n))
                      (nil? (right-child n))))

(defn as-map [n]
    (if (seq? n)
        ; Stringify :sum and filter out key->nil mappings.
        (into {} (filter val {:data (update-in (first n) [:sum]
                                               format-min-dp)
                              :left (second n)
                              :right (nth n 2 nil)}))
        n)) ; Acting like 'identity' on non-maps allows use with postwalk.

(defn- path-to-uid
  "Given a unique user ID to find and a (sub)tree to find it in,
  return a Sequential of :left/:rights giving directions to it, or nil
  if the uid isn't present."
  [uid node]
    (if (leaf? node)
        (if (= uid (:uid (node-data node)))
            '()) ; Empty path is truthy and ready to be prepended to.
        (let [found (lazy-cat [[:left (path-to-uid uid (left-child node))]]
                              [[:right (path-to-uid uid (right-child node))]])]
            (if-let [pair (some #(if (second %) %) found)]
                (apply cons pair)))))

(defn- sha256-hex [s]
    (let [d (java.security.MessageDigest/getInstance "SHA-256")]
        (do
            (.update d (.getBytes s))
            (str/join (map (partial format "%02x") (.digest d))))))

; Combining of plain maps.
(defn- hcombine
  ([n] n)
  ([left right]
    (let [sum (+ (:sum left) (:sum right))]
        {:sum  sum
         :hash (->> [(format-min-dp sum) (:hash left) (:hash right)]
                    (str/join "|")
                    sha256-hex)})))

; Combining of tree nodes which have maps as data.
(defn- ncombine
  ([n] n)
  ([left right]
    (list (hcombine (node-data left) (node-data right)) left right)))

(defn- add-leaf-hash
  [{uid :uid, nonce :nonce, :as leaf}]
  (let [balance (or (:balance leaf) (:sum leaf))]
    (->> (sha256-hex (str uid "|" (format-min-dp balance) "|" nonce))
         (assoc leaf :hash))))

(defn- prep-account
  "Validate, remove irrelevant keys, add :nonce, add :hash, rename
  :balance to :sum."
  [account]
    (validate-account-map account)
    (-> (select-keys account [:uid :balance :nonce])
        add-nonce-if-missing
        add-leaf-hash
        (set/rename-keys {:balance :sum})))

(defn- validate-account-map
  "Ensure account contains the right k/v pairs of the right types."
  [{balance :balance, uid :uid, :as account}]
    (if (or (nil? uid) (not (string? uid))
            (nil? balance) (not (instance? BigDecimal balance)))
        (throw (IllegalArgumentException.
                "Each account in list must have uid->string and balance->bigdec")))
    ; FIXME: Should specifically check it's hex, or if we're relaxed, at least
    ; exclude "|" (well, include everything but "|").
    (if (and (contains? account :nonce) (not (string? (:nonce account))))
        (throw (IllegalArgumentException.
                 (format "account %s: nonce must be a string if present"
                         uid)))))

(defn- add-nonce-if-missing
  "Update given account map adding a random nonce."
  [account]
    (if-not (contains? account :nonce)
        (assoc account :nonce (make-nonce-hexstr (/ nonce-bits 8)))
        account))

(defn- make-nonce-hexstr [num-bytes]
    (let [sr (java.security.SecureRandom.)
          bytes (byte-array num-bytes)]
        (.nextBytes sr bytes)
        (str/join (map (partial format "%02x") bytes))))

(defn- add-dummy-accounts
  "For generating deterministic trees for testing.  Given a
  `Sequential` of preprocessed accounts, append enough dummy accounts
  to pad the list to the next highest power of 2.  Return the padded
  `Sequential`."
  [accounts]
    (let [have (count accounts)
          want (->> (/ (Math/log have) (Math/log 2))
                    Math/ceil
                    (.pow 2M)
                    int)
          num-dummies (- want have)
          dummy (add-leaf-hash {:uid "dummy", :sum 0M, :nonce "0"})]
        (concat accounts (repeat num-dummies (list dummy)))))

(defn- pp-accounts->tree
  "As for `accounts->tree` but takes *preprocessed* account maps, each
  with (at least) the keys :hash and :sum."
  [accounts deterministic]
    (if deterministic
        (if (every? #(contains? (node-data %) :nonce) accounts)
            (pp-accounts->tree-deterministic (add-dummy-accounts accounts))
            ; FIXME will never trigger because preprocessing added nonces.
            (throw (IllegalArgumentException.
                    "In deterministic mode all accounts must already have nonces.")))
        (list (pp-accounts->tree-random accounts))))

(defn- pp-accounts->tree-deterministic [accounts]
    (if (first accounts)
        (if (second accounts)
            ; Group into pairs; replace pairs with their parents/combinations;
            ; repeat.  Because (combine n) => n, stragglers get deferred until
            ; they can be grouped at a shallower depth.  This approach results
            ; in a "full" binary tree; it's simple and minimal, although after
            ; seeing a few verification paths, a customer could have a
            ; reasonable guess at the total number of customers.
            (recur (map (partial apply ncombine) (partition-all 2 accounts)))
            accounts)))

; Approach taken from:
;     https://en.wikipedia.org/wiki/Random_binary_tree#Random_split_trees
(defn- pp-accounts->tree-random
  ([accounts]
    (->> (shuffle accounts)
         gaussian-split
         (apply pp-accounts->tree-random)))
  ([left right]
    (letfn [(get-child [coll] (if (> (count coll) 1)
                                  (->> (gaussian-split coll)
                                       (apply pp-accounts->tree-random))
                                  (first coll)))]
      (ncombine (get-child left) (get-child right)))))

; Helper for verification-path's minimal format.
(defn- combiner-reducer [ndata [sibling-side sibling-data]]
    (if (= sibling-side :left)
        (hcombine sibling-data ndata)
        (hcombine ndata sibling-data)))

(def z99
  "The z value for which (an arbitrary) 99% of randomly picked
  Gaussian-distributed numbers are expected to lie within z standard
  deviations of the mean, i.e. within interval (-zσ, zσ).  This is
  used to constrain `rand-gaussian` to 0 < x < 1 (crudely!) without
  causing >1% of outliers to (be expected to) cluster at the edges."
  (/ 0.5 2.575829))

(defn- gaussian-split
  "Generate a Gaussian-distributed random number and use it to split
  `coll` into two somewhat unequal (but never empty) parts."
  [coll]
    (-> (util/rand-gaussian 0.5 z99 0 1)
        (* (- (count coll) 2))
        math/round
        inc
        (split-at coll)))
