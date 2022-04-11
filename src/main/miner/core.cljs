(ns miner.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [instaparse.core :as insta :refer-macros [defparser]]
            [clojure.string]))

(defonce input (r/atom ""))
(defonce output (r/atom ""))

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

      (do
        (println (str "Cannot eval: " ast))
        (throw (js/Error (str "Cannot eval: " ast)))))))


;;
;; Parse
;;

;; order of operations
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

add = w <'+'> w
sub = w <'-'> w
mul = w <'*'> w
div = w <'/'> w

<amount> = num | wordNum | negWordNum

num = #'-?[1-9][0-9]*'

wordNum = wordNumPart (comma wordNumPart)*
negWordNum = <neg> wordNum
neg =  w? <'minus'> w?
<comma> = <','> w

<wordNumPart> = onePart | thousandPart | millionPart
millionPart = wordNumPlace w <million>
thousandPart = wordNumPlace w <thousand>
onePart = wordNumPlace
million = <('million' | 'miIIion')>
thousand = <'thousand'>

<wordNumPlace> = hundreds? w? (tensAndOnes | tens | ones | teens)?

hundreds = wordNumDigit hundred
<hundred> = w <'hundred'>

<tensAndOnes> = tens (w|<'-'>) ones

tens = wordNumTensDigit
ones = wordNumDigit
teens = teensDigit

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

(defn round [n]
  (/
   (js/Math.round
    (* (+ n js/Number.EPSILON) 100))
   100))

(defn solve []
  (reset! output (-> @input
                     (bonkle-parser)
                     (eval-bonkle)
                     (round)
                     (str))))

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


;;
;; Some example input for testing
;;

;; What is 4909646 - two miIIion, sixty-nine thousand, two hundred seventeen - minus one miIIion, six hundred twenty-six thousand, one hundred eighty-two - 5745712 rounded to 2 decimal places?

;; What is 6662871 + -8990199 rounded to 2 decimal places?

;; What is minus two miIIion, four hundred one thousand, five hundred fifty-two + minus six miIIion, five hundred thirty-nine thousand, seven hundred five * minus eight million, three hundred forty thousand, eight hundred five rounded to 2 decimal places?

;; What is -5498253 + minus seven miIIion, one hundred fifty-five thousand, seven hundred eighty - 2240046 / two miIIion, eight hundred sixty-three thousand, seven hundred fifty + -8602118 / 2883920 rounded to 2 decimal places?

;; What is eight miIIion, nine hundred ninety-three thousand, nine hundred fifty / five miIIion, nine hundred fifty-five thousand, three hundred four - one miIIion, five hundred twenty-two thousand, six hundred ninety-two * minus five million, fifty-two thousand, six hundred forty-four rounded to 2 decimal places?
