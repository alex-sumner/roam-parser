(ns rtree (:require [markers :refer [get-start-marker get-end-marker]]
                    [tidy :refer [tidy-tree]]))

(def rtree->string)

(defn mark-up
  [rtree]
  (let [k (first rtree)]
    (if (keyword? k)
      (str (get-start-marker k)
           (rtree->string (subvec rtree 1))
           (get-end-marker k))
      (apply str
             (for [e rtree]
               (rtree->string e))))))

(defn rtree->string
  "Convert a parsed Roam tree in Hiccup format back to text"
  [rtree]
  (cond
    (vector? rtree) (mark-up rtree)
    (string? rtree) rtree
    :else nil))

(rtree->string [:done])

(def rtree->rtree)

(defn swap-in
  [swap-map rtree]
  (let [k (first rtree)]
    (if (keyword? k)
      (let [subs (if-let [s (swap-map k)] s k)]
        (into [subs] (rtree->rtree swap-map (subvec rtree 1))))
      (into [] (for [e rtree]
                 (rtree->rtree swap-map e))))))

(defn rtree->rtree
  "Transform a parsed Roam tree, swapping elements as specfied in swap-map"
  [swap-map rtree]
  (cond
    (vector? rtree) (swap-in swap-map rtree)
    (string? rtree) rtree
    :else nil))

(def strip-rtree)

(defn strip-out
  [remove-set rtree]
  (let [k (first rtree)]
    (if (keyword? k)
      (if (remove-set k)
        (if (#{:link :alias :image-alias} k)
          []
          (into [] (strip-rtree remove-set (subvec rtree 1))))
        (vector (into [k] (strip-rtree remove-set (subvec rtree 1)))))
      (vector (into [] (for [e rtree]
                         (strip-rtree remove-set e)))))))

(defn strip-element
  [remove-set rtree]
  (cond
    (vector? rtree) (strip-out remove-set rtree)
    (string? rtree) [rtree]
    (keyword? rtree) (if (remove-set rtree)
                       []
                       [rtree])
    :else nil))

(defn strip-rtree
  "Transform a parsed Roam tree, stripping out all elements in remove-set"
  [remove-set rtree]
  (cond
    (vector? rtree) (->> rtree
                         (mapcat (partial strip-element remove-set))
                         vec
                         tidy-tree)
    (string? rtree) rtree
    :else nil))
