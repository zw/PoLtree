PoLtree
=======

An implementation of [Greg Maxwell's Merkle approach][merkle] to [proving
Bitcoin liabilities][proving], in Clojure.

Since I imagine this is most likely to be used directly from other Clojure code
if used at all, the command line interface only really supports simple testing
right now.  The Clojure interface is in [core.clj](/zw/PoLtree/blob/master/src/uk/me/iwilcox/poltree/core.clj#L20).

I don't do lein/maven/gradle, so libraries used were:
 * Clojure 1.4
 * [data.json](/clojure/data.json) 0.2.4

Licence: [Boost Software License Version 1.0][bsl1] (a BSD/MIT-like,
[permissive][perm]-and-[GPL-compatible][fsf-bsl] one).

To Do
=====

* generate random/deterministic trees based on flag (currently always
  deterministic but not in a way that matches the [spec][s11n]).
* more validation of parameters
* more tests (including running on [Olivier Lalonde's test data][oltest])
* more serialisation (probably to/from [formats proposed by Olivier Lalonde][s11n])

 [merkle]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves#merkle_top
 [proving]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves
 [perm]: https://en.wikipedia.org/wiki/Permissive_free_software_licence
 [fsf-bsl]: https://www.gnu.org/licenses/license-list.html#boost
 [bsl1]: http://www.boost.org/LICENSE_1_0.txt
 [oltest]: /olalonde/blind-liability-proof/blob/master/test/accounts.json
 [s11n]: /olalonde/blind-liability-proof#serialized-data-formats-work-in-progress--draft
