#!/bin/bash
#
# Wrapper script to start PoLtree.
#
# Copyright 2014 Isaac Wilcox.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

for j in lib/*.jar; do
    LIB=$LIB:$j
done

clojure1.4 -cp src:test:$LIB --main uk.me.iwilcox.poltree.cli $@
