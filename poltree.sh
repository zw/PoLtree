#!/bin/bash
#
# Wrapper script to start PoLtree.
#

for j in lib/*.jar; do
    LIB=$LIB:$j
done

clojure1.4 -cp src:test:$LIB --main uk.me.iwilcox.poltree.cli $@
