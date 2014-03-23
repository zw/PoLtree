classpathify = $(subst $(eval) ,:,$(wildcard $1))
CLASSPATH = $(call classpathify,lib/*.jar)

.PHONY: test
test:
	clojure -cp src:$(CLASSPATH) test/uk/me/iwilcox/poltree_test.clj
