# roam-parser
Parser for text exported from Roam databases

Takes a text string as exported from a Roam database and builds a tree representing it in Hiccup format (see https://github.com/weavejester/hiccup). 

Based on the specification here: https://roamresearch.com/#/app/help/page/NYgRwJaQM

##### Usage:

`(parser/parse "Some text containing  an __italic section__ and a [[[[nested]]link]]")`

produces:

`[:content "Some text containing  an " [:italic "italic section"] " and a " [:link [:link "nested"] "link"]]`

The parsing conforms to the grammar specified in the file `roam.bnf` in the src root.
For more details on the format of this file see: https://github.com/Engelberg/instaparse/blob/master/docs/ABNF.md

The root element of the tree returned is `[:content ...]`
and its children are plain text strings mixed with any of the following elements.

`:todo :done :link :code-block :code :block-ref :roam-render :latex :image-alias :alias :highlight :bold :italic`

These elements may be nested in accordance with the rules of
the grammar specified in roam.bnf and the results may be modified
by editing that file. If new element types are added to Roam in the future
then the corresponding marker strings used to delineate them must be added
there and also in `markers.clj`

##### Some examples of configuration of the output possible by editing roam.bnf

To remove the :content keyword in the root element add angle brackets round it on line 1:  

`<content> = (element | plain-text)*`

To allow only nested curly braces, but not bold or italic etc, within roam render sections,  
change text to plain-text in line 16:  

`roam-render = <roam-render-start> (roam-render | plain-text)* <roam-render-end>`

##### How it works

The parsing is in 3 phases.
* phase 1 is a regex search and replace that prepares the string for more efficient handling by the main parser
* phase 2 is the main parsing phase and uses the instaparse parser (see: https://github.com/Engelberg/instaparse)
* phase 3 tidies up the resulting tree

So, starting with the string:

`"Text with a {{formula}} (in **__latex__**): $$a^{(1)} = x^t$$"`

phase 1 will find and hlghlight the marker strings producing this:  

`"Text with a ^R^Q{{^S^Rformula^R^Q}}^S^R (in ^R^Q**^S^R^R^Q__^S^Rlatex^R^Q__^S^R^R^Q**^S^R^R^Q)^S^R: ^R^Q$$^S^Ra^{(1)} = x^t^R^Q$$^S^R"`  

which phase 2 will parse as:

`[:content
  "Text with a "
  [:roam-render "formula"]
  " (in "
  [:bold [:italic "latex"]]
  [:alias-end]
  ": "
  [:latex "a^{(1)} = x^t"]]`

This is mostly as we would wish, except that the closing parenthesis after the bold italic word "latex" has been interpreted as marking
the end of a Roam alias, which is wrong. Note that this has not happened to the closing parenthesis after the number 1
in the following formula, this is because that is inside a latex section (between $$ markers) which was enough for phase 1 to not highlight it.

Phase 3 now tidies up, converting the unmatched link marker back to plain text and merging it with the adjacent text to
produce:

`[:content
  "Text with a "
  [:roam-render "formula"]
  " (in "
  [:bold [:italic "latex"]]
  "): "
  [:latex "a^{(1)} = x^t"]]`
