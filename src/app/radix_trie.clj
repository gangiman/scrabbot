(ns radix
  (:require [clojure.string :as string]))
(use 'clojure.java.io)
(use 'clojure.pprint)

(println "Loading names... ")
(time (def names
    (with-open 
      [rdr (reader 
            "/usr/share/dict/ProperNames")]
      (map string/lower-case 
           (doall 
            (take 30000 (line-seq rdr)))))))

(println "Loading words... ")
(time (def words
    (with-open 
      [rdr (reader 
            "/usr/share/dict/words")]
      (map string/lower-case 
           (doall 
            (line-seq rdr))))))

;; From closure.contrib, but missing from clojure 1.4.0 for some reason.
(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))
    
(defn word-seq
    [word]
    (map-indexed (fn [i l] (subs word 0 (inc i))) word))

(defn lookup-tree
    [T word & {:keys [parentKeys] :or {parentKeys []}}]
    (if-let 
        [matchedPrefix (first (filter (fn [prefix] (get T prefix)) (word-seq word)))]
        (let [subT (get T matchedPrefix)]
            ; (println "Match: " matchedPrefix (get T matchedPrefix) (conj parentKeys matchedPrefix))
            (lookup-tree subT 
                (subs word (count matchedPrefix)) 
                :parentKeys (conj parentKeys matchedPrefix)))
        (do ;(println "No match: " word)
            [T (conj parentKeys word)])))
            
(defn common-prefix-length
    [word1 word2]
    (loop [i 0]
        (if (= (get word1 i) (get word2 i))
            (recur (inc i))
            i)))

(defn common-sibling
    [subT parentKeys]
    ; (println "Common siblings? " (keys subT) parentKeys)
    (if-let [siblingKeys (keys subT)]
        (if-let [sibling (first (filter 
                                 (fn [key] (< 0 (common-prefix-length key (last parentKeys))))
                                 siblingKeys))]
            sibling)))

(defn insert
    [T word]
    (let [[subT parentKeys] (lookup-tree T word)]
        ; (println "\nInserting " word parentKeys subT)
        (if-let [sibling (common-sibling subT parentKeys)]
            (let [commonPrefixLength (common-prefix-length (last parentKeys) sibling)
                  taillessParentKeys (vec (take (dec (count parentKeys)) parentKeys))
                  newKeys (conj taillessParentKeys
                                (subs (last parentKeys) 0 commonPrefixLength))
                  oldKeys (conj taillessParentKeys
                                sibling)]
                    ; (println "New keys: " newKeys oldKeys sibling)
                    (assoc-in (dissoc-in T oldKeys) newKeys {
                        (subs (last parentKeys) commonPrefixLength) {}
                        (subs sibling commonPrefixLength) {}
                    }))
            (assoc-in T parentKeys {}))))

(defn assemble-nodes
    [tree prefix]
    (flatten (map (fn [t]
        (if (empty? (tree t))
            (apply str (concat prefix t))
            (assemble-nodes (tree t) (concat prefix t))))
        (keys tree))))

(defn lookup
    [T word & {:keys [limit] :or {limit 10}}]
    (println "\nLooking up: " word)
    (let [word (string/lower-case word)
          tree (get (lookup-tree T word) 0)]
          (take limit (reverse (assemble-nodes tree word)))))
            
(println "Building names trie... ")
(time (def N (reduce insert {} names)))

(println "Building words trie... ")
(time (def W (reduce insert {} words)))

(pprint (lookup N "Al"))
(pprint (lookup N "Bo"))
(pprint (lookup W "Intell" :limit 3))
(pprint (lookup W "Ear" :limit 3))


