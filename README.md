PoLtree
=======

An implementation of [Greg Maxwell's Merkle approach][merkle] to [proving
Bitcoin liabilities][proving], in Clojure.

Licence: [Boost Software License Version 1.0][bsl1] (a BSD/MIT-like,
[permissive][perm]-and-[GPL-compatible][fsf-bsl] one).

To Do
=====

* tests (including running on [Olivier Lalonde's test data][oltest])
* serialisation (probably to/from [formats proposed by Olivier Lalonde][s11n])

 [merkle]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves#merkle_top
 [proving]: https://iwilcox.me.uk/2014/proving-bitcoin-reserves
 [perm]: https://en.wikipedia.org/wiki/Permissive_free_software_licence
 [fsf-bsl]: https://www.gnu.org/licenses/license-list.html#boost
 [bsl1]: http://www.boost.org/LICENSE_1_0.txt
 [oltest]: /olalonde/blind-liability-proof/blob/master/test/accounts.json
 [s11n]: /olalonde/blind-liability-proof#serialized-data-formats-work-in-progress--draft
