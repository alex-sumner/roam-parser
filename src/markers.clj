(ns markers
  (:require [clojure.string :as st]
            [clojure.set :refer [map-invert]]))

;; Map whose keys are all the strings we need to mark up before the main parsing starts.
(def marker->keyword {"{{[[TODO]]}}" :todo
                      "{{[[DONE]]}}" :done
                      "[[" :link-start
                      "]]" :link-end
                      "```" :code-block
                      "`" :code
                      "((" :block-ref-start
                      "))" :block-ref-end
                      "{{" :roam-render-start
                      "}}" :roam-render-end
                      "$$" :latex
                      "![" :image-alias-start
                      "[" :alias-start
                      "](" :alias-mid
                      ")" :alias-end
                      "^^" :highlight
                      "**" :bold
                      "__" :italic})

(def keyword->marker (map-invert marker->keyword))

;; A map whose keys are all the characters that both have a special meaning in a regular expression
;; and also occur in any of the marker strings defined in marker->keyword. These characters must be
;; escaped in the regex string and the map values are the escaped versions of them.
(def chars-to-escape {\` "\\`" \{ "\\{" \} "\\}" \[ "\\[" \] "\\]" \( "\\(" \) "\\)" \^ "\\^" \$ "\\$" \* "\\*"})

;; List of masking marker types and atoms to track keep track of whether we are in them.
;; Each type masks all other marker types except ones before it in the list.
;; So code blocks (```...```) mask everything, whereas code sections (`...`)
;; mask everything except code blocks, etc.
(def masking-blocks [{:marker :code-block
                      :in-it (atom false)}
                     {:marker :code
                      :in-it (atom false)}
                     {:marker :latex
                      :in-it (atom false)}])

;; called at the start of the regex search and replace to reset the state of our atoms
(defn init
  []
  (for [mb masking-blocks]
    (reset! (:in-it mb) false)))

;; given one of the marker strings build a substitute for it that the parser can find easily and unambiguously
(defn make-substitute-for
  [original]
  (str \u0012 \u0011 original \u0013 \u0012))

;; Recursively work through the masking block types to determine whether the marker string should be
;; substituted (so the parser will see it) or used as is (because it is masked by an enclosing
;; string, such as when it is within a code block, so the parser should treat it as plain text).
(defn choose-with-masking
  [k original masks]
  (if (empty? masks)
    (make-substitute-for original) ;; we've checked all the masking types, so can safely substitute
    (let [{:keys [marker in-it]} (first masks)
          we-are-in-it @in-it]
      (if (= k marker)
        (do ;; we are at the start or end of an occurence of the current type
          (swap! in-it not)
          (make-substitute-for original))
        (if @in-it
          original ;; markers of other types are ignored within the block
          (recur k original (next masks)))))))

;; We have found one of the marker strings and can either substitute for it,
;; in which case the parser will see it as a marker, or use the original text, in which case
;; the parser will treat it as plain text. The latter option is what happens if, for instance,
;; we come across a $$ or ^^ inside a code block.
(defn choose-substitute-for
  [k original]
  (choose-with-masking k original masking-blocks))

;; Called once for each match during the regex replace operation,
;; looks up the substitution for the matched marker string.
(defn replace-marker
  [match-data]
  (let [original (first match-data)
        k (marker->keyword original)]
    (if (nil? k)
      original
      (choose-substitute-for k original))))

;; Build the regex pattern from the escaped marker strings. Put the longest
;; strings first as we want them to match first.
(def regex (re-pattern (str "("
                            (apply str
                                   (->> marker->keyword
                                        keys
                                        (sort-by #(- (count %)))
                                        (map #(st/escape % chars-to-escape))
                                        (interpose \|)))
                            ")")))

;; Takes the raw string to be parsed, returns the string that will be passed to the parser.
(defn pre-parse
  [s]
 (init)
  (st/replace s regex replace-marker))

