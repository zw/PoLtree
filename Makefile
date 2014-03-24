# Copyright 2014 Isaac Wilcox.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

classpathify = $(subst $(eval) ,:,$(wildcard $1))
CLASSPATH = $(call classpathify,lib/*.jar)

.PHONY: test
test:
	clojure -cp src:$(CLASSPATH) test/uk/me/iwilcox/poltree_test.clj
