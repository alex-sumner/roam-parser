###Parser for text exported from Roam databases, see https://roamresearch.com/

Based on the specification here: https://roamresearch.com/#/app/help/page/NYgRwJaQM

Usage:

(parser/parse "Some text containing  an __italic section__ and a [[[[nested]]link]]")

produces:

[:content "Some text containing  an " [:italic "italic section"] " and a " [:link [:link "nested"] "link"]]

Uses the grammar specified in the file roam.bnf in the src root
for more details see: https://github.com/Engelberg/instaparse/blob/master/docs/ABNF.md

The root element of the tree returned is [:content ...]
and its children are plain text strings mixed with any of the following elements.

:todo :done :link :code-block :code :block-ref :roam-render
:latex :image-alias :alias :highlight :bold :italic

These elements may be nested in accordance with the rules of
the grammar specified in roam.bnf and the results may be modified
by editing that file. If new element types are added to Roam in the future
then the corresponding marker strings used to delineate them must be added
there and also in markers.clj

Some examples of configuration of the output possible by editing roam.bnf

    To remove the :content keyword in the root element add angle brackets round it on line 1:

    <content> = (element | plain-text)*

    To allow only nested curly braces, but not bold or italic etc, within roam render sections,
    change text to plain-text in line 16:

    roam-render = <roam-render-start> (roam-render | plain-text)* <roam-render-end>

The parsing is in 3 phases.
* phase 1 is a regex search and replace that prepares the string for more efficient handling by the main parser
* phase 2 is the main parsing phase and uses the instaparse parser (see: https://github.com/Engelberg/instaparse)
* phase 3 tidies up the resulting tree
