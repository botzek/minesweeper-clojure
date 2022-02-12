(ns minesweeper.app
  (:require
    [minesweeper.core :as core]
    [cljs.spec.alpha :as s]
    [expound.alpha :as expound]
    #_[devtools.core :as devtools]))

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(set! s/*explain-out* expound/printer)

(enable-console-print!)

#_(devtools/install!)

(core/init!)
