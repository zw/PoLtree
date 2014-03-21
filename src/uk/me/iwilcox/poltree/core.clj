; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; Distributed under the Boost Software License, Version 1.0.  See accompanying
; file LICENCE.txt or copy at <http://www.boost.org/LICENSE_1_0.txt>.
;
; Core interface/data types.
;
(ns uk.me.iwilcox.poltree.core
    (:require [clojure.string :as str])
    (:require [clojure.set :as set]))

(declare node-data left-child right-child leaf?)
(declare sha256-hex hcombine ncombine leaf-hash)
(declare prep-account pp-accounts->tree)
(declare combiner-reducer)

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
(defn accounts->tree [accounts]
    ; For each account, adapt keys then nest in a list to make it a tree leaf
    ; node.
    (-> (map #(list (prep-account %)) accounts)
        (pp-accounts->tree ,,,)
        (first ,,,)))

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
  ([root root-path] (verification-path root root-path ()))
  ([n root-path vpath]
    (if (leaf? n)
        (cons (:nonce (node-data n)) vpath)
        (let [goleft (= :left (first root-path))
              follow-child ((if goleft left-child right-child) n)
              other-child-side (if goleft :right :left)
              other-child ((if goleft right-child left-child) n)
              other-child-data (select-keys (node-data other-child) [:sum :hash])]
            (->> (cons [other-child-side other-child-data] vpath)
                 (recur follow-child (rest root-path) ,,,))))))

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
        (and (not-any? #(neg? (:sum (second %))) vpath)
             (= published-root-hash
                (:hash (reduce combiner-reducer account vpath))))))

;;;;;;;;;;;
; Helpers
;;;;;;;;;;;
(declare add-nonce-if-missing make-nonce-hexstr validate-account-map)

(defn- format-min-dp
  "Format `sum` (a bigdec) with the minimum possible number of
  trailing zeros."
  [sum]
    (.toPlainString (.stripTrailingZeros sum)))

; Convenience bits for manipulating tree nodes of the form:
;     ( { <data> }  <left child>  <right child> )
(defn- node-data [n] (first n))
(defn- left-child [n] (second n))
(defn- right-child [n] (nth n 2 nil)) ; aka third
(defn- leaf? [n] (and (nil? (left-child n))
                      (nil? (right-child n))))
; Or:       [n] (every? nil? (rest n))
; Or:       [n] (:uid n)

(defn as-map [n]
    (if (seq? n)
        ; Stringify :sum and filter out key->nil mappings.
        (into {} (filter val { :data (update-in (first n) [:sum]
                                                format-min-dp)
                               :left (second n)
                               :right (nth n 2 nil) }))
        n)) ; Acting like 'identity' on non-maps allows use with postwalk.

(defn- sha256-hex [s]
    (let [d (java.security.MessageDigest/getInstance "SHA-256")]
        (do
            (.update d (.getBytes s))
            (str/join (map #(format "%02x" %) (.digest d))))))

; Combining of plain maps.
(defn- hcombine
  ([n] n)
  ([l r]
    (let [sum (+ (:sum l) (:sum r))]
        {:sum  sum
         :hash (->> [ (format-min-dp sum) (l :hash) (r :hash) ]
                    (str/join "|" ,,,)
                    (sha256-hex ,,,))})))

; Combining of tree nodes which have maps as data.
(defn- ncombine
  ([n] n)
  ([l r] (list (hcombine (node-data l) (node-data r)) l r)))

(defn- leaf-hash [{uid :uid, balance :balance, nonce :nonce}]
    (sha256-hex (str uid "|" (format-min-dp balance) "|" nonce)))

(defn- prep-account
  "Validate, then remove irrelevant keys, add :nonce, add :hash,
  rename :balance to :sum."
  [account]
    (validate-account-map account)
    (-> (select-keys account [:uid :balance :nonce])
        (add-nonce-if-missing ,,,)
        (assoc ,,, :hash (leaf-hash account))
        (set/rename-keys ,,, {:balance :sum})))

(defn- validate-account-map
  "Ensure account contains the right k/v pairs of the right types."
  [{ balance :balance, uid :uid, :as account }]
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
        (str/join (map #(format "%02x" %) bytes))))

; Build a binary Merkle tree of liabilities from a collection of preprocessed
; account maps, each with (at least) the keys :hash and :sum.
(defn- pp-accounts->tree [accounts]
    (if (first accounts)
        (if (second accounts)
            ; Group into pairs; replace pairs with their parents/combinations;
            ; repeat.  Because (combine n) => n, stragglers get deferred until
            ; they can be grouped at a shallower depth.  This approach results
            ; in a "full" binary tree; it's simple and minimal, although after
            ; seeing a few verification paths, a customer could have a
            ; reasonable guess at the total number of customers.
            (recur (map #(apply ncombine %) (partition-all 2 accounts)))
            accounts)))

; Helper for verification-path's minimal format.
(defn- combiner-reducer [ndata [sibling-side sibling-data]]
    (if (= sibling-side :left)
        (hcombine sibling-data ndata)
        (hcombine ndata sibling-data)))
