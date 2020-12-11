(ns examples
  (:require [parser :refer [parse]]
            [rtree :refer [rtree->string rtree->rtree strip-rtree]]))

;; Some examples of transformations on parsed Roam trees


;; create an intial text string to work with
(def initial-text "{{[[TODO]]}} a [[link]] and __then__ **__two aliases__**:[x](y) ![i](j)")

;; parse it...
(parse initial-text)
;; =>
;; [:content
;;  [:todo]
;;  " a "
;;  [:link "link"]
;;  " and "
;;  [:italic "then"]
;;  " "
;;  [:bold [:italic "two aliases"]]
;;  ":"
;;  [:alias "x" [:alias-link "y"]]
;;  " "
;;  [:image-alias "i" [:alias-link "j"]]]

;; supply a map defining how we wish to transform it,
;; here we are going to replace Roam links with Roam renders,
;; italic sections with bold sections,
;; bold sections with highlighted sections
;; and TODOs with DONEs:
(def transform-map {:link :roam-render
                    :italic :bold
                    :bold :highlight
                    :todo :done})

;; apply this transformation
(->> initial-text
     parse
     (rtree->rtree transform-map))
;; =>
;; [:content
;;  [:done]
;;  " a "
;;  [:roam-render "link"]
;;  " and "
;;  [:bold "then"]
;;  " "
;;  [:highlight [:bold "two aliases"]]
;;  ":"
;;  [:alias "x" [:alias-link "y"]]
;;  " "
;;  [:image-alias "i" [:alias-link "j"]]]

;; next convert back to a string
(->> initial-text
     parse
     (rtree->rtree transform-map)
     rtree->string)
;; =>
;; "{{[[DONE]]}} a {{link}} and **then** ^^**two aliases**^^:[x](y) ![i](j)"
;;
;; comparison of our original string with the tansformed one...
;;
;; original:    "{{[[TODO]]}} a [[link]] and __then__ **__two aliases__**:[x](y) ![i](j)"
;; transformed: "{{[[DONE]]}} a {{link}} and **then** ^^**two aliases**^^:[x](y) ![i](j)"

;; now strip the DONE, bold and image alias:
(->> initial-text
     parse
     (rtree->rtree transform-map)
     (strip-rtree #{:bold :done :image-alias}))
;; =>
;; [:content
;;  " a "
;;  [:roam-render "link"]
;;  " and then "
;;  [:highlight "two aliases"]
;;  ":"
;;  [:alias "x" [:alias-link "y"]]
;;  " "]


;; and convert back to text again:
(->> initial-text
     parse
     (rtree->rtree transform-map)
     (strip-rtree #{:bold :done :image-alias})
     rtree->string)
;; =>
;; " a {{link}} and then ^^two aliases^^:[x](y) "

