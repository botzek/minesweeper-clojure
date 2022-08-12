(ns minesweeper.app
  (:require [minesweeper.core :as core]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]))

(if goog.DEBUG
  (do
    (extend-protocol IPrintWithWriter
      js/Symbol
      (-pr-writer [sym writer _]
        (-write writer (str "\"" (.toString sym) "\""))))

    (set! s/*explain-out* expound/printer)

    (enable-console-print!))
  (do
    ;;ignore println statements in prod
    (set! *print-fn* (fn [& _]))))
(core/init!)
