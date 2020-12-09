(ns tidy
  (:require [markers :refer [keyword->marker]]))

;; The element types that will be removed from the tree in the third phase of the parse.
(def elements-to-tidy #{:image-alias-start :alias-start :alias-mid :alias-end})

;; If given an element of any of the types in elements-to-tidy, return the corresponding
;; orignal marker string as defined in markers.clj. If given anything else, just return
;; what we were given.
(defn fragment-to-string
  [e]
  (if (vector? e)
    (let [[e-type] e]
      (if (elements-to-tidy e-type)
        (keyword->marker e-type)
        e))
    e))

(defn try-to-merge
  [previous current]
  (if (string? previous)
    (str previous current)))

;; Replace the last thing added to collection
(defn replace-previous [coll replacement]
  (conj (pop coll) replacement))

;; If the next item is something to be tidied, replace it with a string.
;; If the item is a string, or was converted to one, and the preceeding
;; item was also a string, then concatenate them. Otherwise use the item as is.
(defn process-next
  [result remaining]
  (let [current (fragment-to-string (first remaining))]
    (if (string? current)
      (let [previous (peek result)
            merged (try-to-merge previous current)]
        (if merged
          (replace-previous result merged)
          (conj result current)))
      (conj result current))))

;; Work recursively through the elements provided, replacing any alias fragments
;; and adding corresponding plain text to the result.
(defn clean-up-fragments
  [result remaining]
  (if (empty? remaining)
    result
    (let [new-result (process-next result remaining)
          new-remaining (next remaining)]
      (recur new-result new-remaining))))

;; Work through the tree replacing any alias fragments, that is,
;; any alias start, mid or end markers which are not matched up to form
;; an alias element. These are converted back to plain text, for example
;; an [:alias-end] element with no matching alias start and mid point
;; will be converted back to the string ")", and this string will be
;; merged with an adjacent string if possible.
(defn tidy-tree
  [tree]
  (clean-up-fragments [] tree))

