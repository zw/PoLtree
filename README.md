PoLtree
=======

An implementation of [Greg Maxwell's Merkle approach][merkle] to [proving
Bitcoin liabilities][proving], in Clojure.

Since I imagine this is most likely to be used directly from other Clojure code
by exchanges if used at all, the command line interface only really supports
simple testing (account list [input][accountlist], complete tree output) right
now.  The Clojure interface is in [core.clj][api].

I don't do lein/maven/gradle.  The libraries/versions I used were as follows
(but earlier versions may very well work):
 * Java 1.7 (Oracle's one)
 * Clojure 1.4
 * [data.json](/clojure/data.json) 0.2.4
 * [math.numeric-tower](/clojure/math.numeric-tower) 0.0.4

Licence: [Boost Software License Version 1.0][bsl1] (a BSD/MIT-like,
[permissive][perm], [GPL-compatible][fsf-bsl] one), except as noted for
borrowed snippets.

To Do
=====

* add support for the root and partial tree [serialisation formats][s11n]
* more validation of parameters
* more tests (including formally running on [Olivier Lalonde's test
  data][oltest])

 [merkle]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves#merkle_top
 [proving]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves
 [perm]: https://en.wikipedia.org/wiki/Permissive_free_software_licence
 [fsf-bsl]: https://www.gnu.org/licenses/license-list.html#boost
 [bsl1]: http://www.boost.org/LICENSE_1_0.txt
 [oltest]: /olalonde/proof-of-liabilities/blob/master/test/accounts.json
 [s11n]: /olalonde/proof-of-liabilities#serialized-data-formats-work-in-progress--draft
 [api]: /zw/PoLtree/blob/master/src/uk/me/iwilcox/poltree/core.clj#L20
 [accountlist]: /olalonde/proof-of-liabilities#account-lists
