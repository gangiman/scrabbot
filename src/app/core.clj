(ns app.core)
(use 'clojure.pprint)

(require '[clojure.data.json :as json])
(require '[clojure.string :as str])

(load-file "src/app/radix_trie.clj")

(defn getGameJson [pathToFile]
	(json/read-str
		(slurp pathToFile) :key-fn keyword))

(def game
	(getGameJson "data/wwf_test_game.json"))


(defn -main []
	(def D_trie (reduce radix/insert {}
		(radix/read_words_from_file "data/full_word_list.csv")))
	(time (pprint (radix/lookup D_trie "hel"))))
