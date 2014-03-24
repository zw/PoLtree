PoLtree
=======

An implementation of [Greg Maxwell's Merkle approach][merkle] to [proving
Bitcoin liabilities][proving], in Clojure.

An example partial graph from the [full example accounts list]
[accounts-json], for `satoshi`:

<img src="https://iwilcox.me.uk/2014/proving/satoshi-partial" />

An example complete graph from a [simpler accounts list] [trivial]:

<img src="https://iwilcox.me.uk/2014/proving/trivial-complete" />

Here's how to reproduce the above graphs using the CLI.  You'll need
[`accounts-deterministic.json`] [accounts-json] and
[`trivial-deterministic.json`] [trivial] to follow along.  The output
is minimal (whitespace-light) so I'd recommend piping it through
[`jq .`] [jq] if you want to read it, but the prettier way to view
results is to render them with [GraphViz] [gv] using the supplied
converter.

 [accounts-json]: https://github.com/zw/blind-liability-proof/blob/e4991c892fd481d3b6f66ae331909fd0c7af6a9d/test/data/accounts-deterministic.json
 [trivial]: https://github.com/zw/blind-liability-proof/blob/e4991c892fd481d3b6f66ae331909fd0c7af6a9d/test/data/trivial-deterministic.json
 [jq]: http://stedolan.github.io/jq/
 [gv]: http://www.graphviz.org/

```shell
./poltree.sh completetree accounts-deterministic.json >complete-tree.json

# Or...

cat trivial-deterministic.json | ./poltree.sh completetree >trivial-complete.json

# Or if you've installed jq (Debian etc: jq) and prefer pretty JSON...

cat trivial-deterministic.json | ./poltree.sh completetree | jq .

# Or if you've installed extra bits (Debian etc: graphviz, libjson-perl)...

cat trivial-deterministic.json | ./poltree.sh completetree \
    | perl tools/s11n-to-dot.pl | dot -Tpng >trivial-complete.png


./poltree.sh partialtree satoshi complete-tree.json >satoshi-partial.json

# You get the idea.  Altogether now...

cat accounts-deterministic.json | ./poltree.sh completetree \
    | ./poltree.sh partialtree satoshi | perl tools/s11n-to-dot.pl \
    | dot -Tpng >satoshi-partial.png
```

Since I imagine this is most likely to be used directly from other Clojure code
by exchanges if used at all, the command line interface only really supports
simple testing (account list [input][accountlist], complete tree output) right
now.  The Clojure interface is in [core.clj][api].

I don't do lein/maven/gradle.  The libraries/versions I used were as follows
(but earlier versions may very well work):
 * Java 1.7 (Oracle's one)
 * Clojure 1.4
 * [data.json](/clojure/data.json), v[0.2.4](http://search.maven.org/#artifactdetails|org.clojure|data.json|0.2.4|jar)
 * [math.numeric-tower](/clojure/math.numeric-tower), v[0.0.4](http://search.maven.org/#artifactdetails|org.clojure|math.numeric-tower|0.0.4|jar)

Licence: [Boost Software License Version 1.0][bsl1] (a BSD/MIT-like,
[permissive][perm], [GPL-compatible][fsf-bsl] one), except as noted for
borrowed snippets.

To Do
=====

* add support for the root [serialisation format][s11n]
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
