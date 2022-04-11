(ns miner.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [instaparse.core :as insta :refer-macros [defparser]]
            [clojure.string]))

(defonce input (r/atom ""))
(defonce output (r/atom ""))

; overlapping matches should go later
; ie: abc > ab > a
(def translations
  [["1" "i"]
   ["3" "e"]
   ["5" "s"]
   ["7" "t"]
   ["0" "o"]
   ["|_|" "u"]
   ["'/" "y"]
   ["|)" "d"]
   ["+" "t"]
   ["|" "l"]])

(defn deobfuscate [s]
  (reduce
   (fn [s translation]
     (clojure.string/replace s (translation 0) (translation 1)))
   s
   translations))

;;
;; Evaluate
;;

(declare bonkle-parser)

(defn eval-bonkle [ast]
  (if-not (vector? ast)
    ast
    (case (ast 0)
      :solution (eval-bonkle (ast 1))
      :total (eval-bonkle (ast 1))

      :addExpr (+ (eval-bonkle (ast 1)) (eval-bonkle (ast 2)))
      :subExpr (- (eval-bonkle (ast 1)) (eval-bonkle (ast 2)))
      :mulExpr (* (eval-bonkle (ast 1)) (eval-bonkle (ast 2)))
      :divExpr (/ (eval-bonkle (ast 1)) (eval-bonkle (ast 2)))

      :num (js/parseFloat (ast 1))
      :wordNum (reduce + (map eval-bonkle (rest ast)))
      :negWordNum (* -1 (eval-bonkle (ast 1)))

      :millionPart (* 1000000 (reduce + (map eval-bonkle (rest ast))))
      :thousandPart (* 1000 (reduce + (map eval-bonkle (rest ast))))
      :onePart (reduce + (map eval-bonkle (rest ast)))

      :hundreds (* 100 (eval-bonkle (ast 1)))
      :tens (* 10 (eval-bonkle (ast 1)))
      :ones (eval-bonkle (ast 1))
      :teens (eval-bonkle (ast 1))

      :one 1
      :two 2
      :three 3
      :four 4
      :five 5
      :six 6
      :seven 7
      :eight 8
      :nine 9

      :eleven 11
      :twelve 12
      :thirteen 13
      :fourteen 14
      :fifteen 15
      :sixteen 16
      :seventeen 17
      :eighteen 18
      :nineteen 19

      :ten 1
      :twenty 2
      :thirty 3
      :fourty 4
      :fifty 5
      :sixty 6
      :seventy 7
      :eighty 8
      :ninety 9

      :obfuscated (eval-bonkle (bonkle-parser (deobfuscate (ast 1))))

      (do
        (println (str "Cannot eval: " ast))
        (throw (js/Error (str "Cannot eval: " ast)))))))


;;
;; Parse
;;

;; What is 4909646 - two miIIion, sixty-nine thousand, two hundred seventeen - minus one miIIion, six hundred twenty-six thousand, one hundred eighty-two - 5745712 rounded to 2 decimal places?

;; What is 6662871 + -8990199 rounded to 2 decimal places?

;; What is minus two miIIion, four hundred one thousand, five hundred fifty-two + minus six miIIion, five hundred thirty-nine thousand, seven hundred five * minus eight million, three hundred forty thousand, eight hundred five rounded to 2 decimal places?

;; What is -5498253 + minus seven miIIion, one hundred fifty-five thousand, seven hundred eighty - 2240046 / two miIIion, eight hundred sixty-three thousand, seven hundred fifty + -8602118 / 2883920 rounded to 2 decimal places?

;; What is eight miIIion, nine hundred ninety-three thousand, nine hundred fifty / five miIIion, nine hundred fifty-five thousand, three hundred four - one miIIion, five hundred twenty-two thousand, six hundred ninety-two * minus five million, fifty-two thousand, six hundred forty-four rounded to 2 decimal places?

[:solution
 [:total
  [:subExpr
   [:divExpr
    [:wordNum
     [:millionPart
      [:ones
       [:eightDigit]]]
     [:thousandPart
      [:hundreds
       [:nineDigit]]
      [:tens
       [:ninety]]
      [:ones
       [:threeDigit]]]
     [:onePart
      [:hundreds
       [:nineDigit]]
      [:tens
       [:fifty]]]]
    [:wordNum
     [:millionPart
      [:ones
       [:fiveDigit]]]
     [:thousandPart
      [:hundreds
       [:nineDigit]]
      [:tens
       [:fifty]]
      [:ones
       [:fiveDigit]]]
     [:onePart
      [:hundreds
       [:threeDigit]]
      [:ones
       [:fourDigit]]]]]
   [:wordNum
    [:millionPart
     [:ones
      [:oneDigit]]]
    [:thousandPart
     [:hundreds
      [:fiveDigit]]
     [:tens
      [:twenty]]
     [:ones
      [:twoDigit]]]
    [:onePart
     [:hundreds
      [:sixDigit]]
     [:tens
      [:ninety]]
     [:ones
      [:twoDigit]]]]]]]


;; TODO order of operations
;; make recursive symbols and put some higher than others
;; let each symbol bottom out to next one by default (go down a level)
;; HIGHER UP == LOWER PRECEDENCE
;; EX:
;;   expr := term | expr “+” term | expr “-“ term
;;   term := atom | term “*” atom | term “/” atom
;; https://cs61.seas.harvard.edu/wiki/images/d/d4/Cs61-2013-l22-scribe2.pdf
;; "Precedence climbing method"
;; https://en.wikipedia.org/wiki/Operator-precedence_parser#Precedence_climbing_method
(defparser bonkle-parser "
solution = w? <'What is'> w total w <'rounded to 2 decimal places?'> w?
<w> = <#'\\s+'>

total = expr1

<expr1> = addExpr | subExpr | expr2
addExpr = expr1 <add> expr1
subExpr = expr1 <sub> expr1

<expr2> = mulExpr | divExpr | amount
mulExpr = expr2 <mul> expr2
divExpr = expr2 <div> expr2

operators = add | sub | mul | div
add = w <'+'> w
sub = w <'-'> w
mul = w <'*'> w
div = w <'/'> w

<amount> = num | wordNum | negWordNum | obfuscated

num = #'-?[1-9][0-9]*'

wordNum = wordNumPart (comma wordNumPart)*
negWordNum = <neg> wordNum
neg =  w? <'minus'> w?
<comma> = <','> w

<wordNumPart> = onePart | thousandPart | millionPart | obfuscated
millionPart = wordNumPlace w <million>
thousandPart = wordNumPlace w <thousand>
onePart = wordNumPlace
million = <('million' | 'miIIion')>
thousand = <'thousand'>

<wordNumPlace> = hundreds? w? (tensAndOnes | tens | ones | teens)?

hundreds = wordNumDigit hundred
<hundred> = w <'hundred'>

<tensAndOnes> = tens (w|<'-'>) ones

obfuscated = #'[\\w|\\\\\\-\\,\\'()_+/]+'

tens = wordNumTensDigit | obfuscated
ones = wordNumDigit | obfuscated
teens = teensDigit | obfuscated

<wordNumDigit> = one | two | three | four | five | six | seven | eight | nine

one = <'one'>
two = <'two'>
three = <'three'>
four = <'four'>
five = <'five'>
six = <'six'>
seven = <'seven'>
eight = <'eight'>
nine = <'nine'>

<wordNumTensDigit> = ten | twenty | thirty | fourty | fifty | sixty | seventy | eighty | ninety

ten = <'ten'>
twenty = <'twenty'>
thirty = <'thirty'>
fourty = <'forty'>
fifty = <'fifty'>
sixty = <'sixty'>
seventy = <'seventy'>
eighty = <'eighty'>
ninety = <'ninety'>

<teensDigit> = eleven | twelve | thirteen | fourteen | fifteen | sixteen | seventeen | eighteen | nineteen

eleven = <'eleven'> | <'eIeven'>
twelve = <'twelve'> | <'tweIve'>
thirteen = <'thirteen'>
fourteen = <'fourteen'>
fifteen = <'fifteen'>
sixteen = <'sixteen'>
seventeen = <'seventeen'>
eighteen = <'eighteen'>
nineteen = <'nineteen'>
")

; -201835218865652300000
; -201835218865652270000

(defn round [n]
  (/
   (js/Math.round
    (* (+ n js/Number.EPSILON) 100))
   100))

(defn solve []
  (println "Solving...")
  (let [ast (bonkle-parser @input)]
    (println ast)
    (println (bonkle-parser @input :total true))
    (reset! output (str (round (eval-bonkle ast))))))

;;
;; Views
;;

(defn inputarea []
  [:textarea {:value @input
              :placeholder "Please enter the bonkle problem here."
              :on-change #(reset! input (-> % .-target .-value))}])

(defn button []
  [:button {:on-click #(solve)}
   "Solve"])

(defn outputarea []
  [:textarea {:value @output
              :readOnly true
              :placeholder "Enter a problem above to get the solution."}])

(defn app []
  [:div
   [:h1 "BonkleBuck Miner"]
   [:p
    [inputarea]
    [:br]
    [button]]
   [:p [outputarea]]])

(defn ^:export run []
  (println "APP RUNNING!!")
  (rdom/render [app] (js/document.getElementById "app")))
