(ns app.core)

(require '[clojure.data.json :as json])
(require '[clojure.string :as str])

(defn getGameJson [pathToFile]
	(json/read-str (slurp pathToFile)
                :key-fn keyword)
)
(def game (:game (getGameJson "wwf_game.json")))

(defn -main []
nil 
)
