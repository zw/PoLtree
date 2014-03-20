CLASSPATH = lib/data.json-0.2.4.jar

.PHONY: test
test:
	clojure -cp src:$(CLASSPATH) test/uk/me/iwilcox/poltree_test.clj
