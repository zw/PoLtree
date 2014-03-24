; An implementation of (the proof-of-inclusion-in-liabilities part of)
; gmaxwell's proof-of-reserves-(non)fractionality system, as described at
; <https://iwilcox.me.uk/2014/proving-bitcoin-reserves>.
;
; Copyright 2014 Isaac Wilcox.
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;
; rand-gaussian is taken from <https://github.com/sjl/roul> 0.2.0.
; Copyright 2012 Steve Losh and Contributors
; MIT/X11 Licenced
;
; Utilities.
;
(ns uk.me.iwilcox.poltree.util)

(def generator (java.util.Random.))

(defn rand-gaussian
  "Return a random float.

  Floats are generated from a Gaussian distribution with the given
  mean and standard deviation.

  A lower and upper bound can be specified if desired, which will
  clamp the output of this function to those bounds. Note that this
  clamping does NOT adjust the distribution, so if you clamp too
  tightly you'll get a disproportionate number of the boundary
  values. It's just here to give you a way to prevent garbage values.

  mean defaults to 0.  standard-deviation defaults to 1."
  ([]
      (.nextGaussian generator))
  ([mean standard-deviation]
      (-> (rand-gaussian)
          (* standard-deviation)
          (+ mean)))
  ([mean standard-deviation lower-bound upper-bound]
      (-> (rand-gaussian mean standard-deviation)
          (max lower-bound)
          (min upper-bound))))

